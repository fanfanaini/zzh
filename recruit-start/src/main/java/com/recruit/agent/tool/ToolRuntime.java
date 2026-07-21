package com.recruit.agent.tool;

import com.recruit.agent.trace.ToolTrace;
import com.recruit.agent.trace.ToolTraceSink;
import com.recruit.agent.trace.ToolTraceStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ToolRuntime implements AutoCloseable {
    private final ToolRegistry registry;
    private final ToolPermissionGuard permissionGuard;
    private final ToolIdempotencyStore idempotencyStore;
    private final ToolTraceSink traceSink;
    private final ExecutorService executor;
    private final boolean ownsExecutor;

    public ToolRuntime(ToolRegistry registry,
                       ToolPermissionGuard permissionGuard,
                       ToolIdempotencyStore idempotencyStore,
                       ToolTraceSink traceSink) {
        this(registry, permissionGuard, idempotencyStore, traceSink,
                Executors.newCachedThreadPool(), true);
    }

    public ToolRuntime(ToolRegistry registry,
                       ToolPermissionGuard permissionGuard,
                       ToolIdempotencyStore idempotencyStore,
                       ToolTraceSink traceSink,
                       ExecutorService executor) {
        this(registry, permissionGuard, idempotencyStore, traceSink, executor, false);
    }

    private ToolRuntime(ToolRegistry registry,
                        ToolPermissionGuard permissionGuard,
                        ToolIdempotencyStore idempotencyStore,
                        ToolTraceSink traceSink,
                        ExecutorService executor,
                        boolean ownsExecutor) {
        if (registry == null || permissionGuard == null || idempotencyStore == null
                || traceSink == null || executor == null) {
            throw new IllegalArgumentException("Tool runtime dependencies must not be null");
        }
        this.registry = registry;
        this.permissionGuard = permissionGuard;
        this.idempotencyStore = idempotencyStore;
        this.traceSink = traceSink;
        this.executor = executor;
        this.ownsExecutor = ownsExecutor;
    }

    public ToolResult invoke(String toolName,
                             Map<String, Object> arguments,
                             ToolInvocationContext context) {
        String traceId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        ToolInvocationContext safeContext = context == null ? ToolInvocationContext.anonymous() : context;
        Map<String, Object> safeArguments = immutableArguments(arguments);

        if (toolName == null || toolName.isBlank()) {
            return finish(traceId, toolName, safeArguments, safeContext, startedAt,
                    failure(traceId, ToolErrorCode.TOOL_NOT_FOUND, "Tool name must not be blank", Map.of(), 0));
        }

        ToolRegistry.RegisteredTool registeredTool = registry.find(toolName).orElse(null);
        if (registeredTool == null) {
            return finish(traceId, toolName, safeArguments, safeContext, startedAt,
                    failure(traceId, ToolErrorCode.TOOL_NOT_FOUND, "Tool is not registered: " + toolName, Map.of(), 0));
        }

        ToolDescriptor descriptor = registeredTool.descriptor();
        List<String> violations = descriptor.parameterContract().validate(safeArguments);
        if (!violations.isEmpty()) {
            return finish(traceId, toolName, safeArguments, safeContext, startedAt,
                    failure(traceId, ToolErrorCode.INVALID_ARGUMENTS, "Tool arguments are invalid",
                            Map.of("violations", violations), 0));
        }

        Set<String> missingPermissions = permissionGuard.missingPermissions(descriptor, safeContext);
        if (!missingPermissions.isEmpty()) {
            return finish(traceId, toolName, safeArguments, safeContext, startedAt,
                    failure(traceId, ToolErrorCode.PERMISSION_DENIED, "Tool permission denied",
                            Map.of("missingPermissions", missingPermissions), 0));
        }

        String idempotencyScope = null;
        if (descriptor.idempotent()) {
            if (safeContext.idempotencyKey() == null) {
                return finish(traceId, toolName, safeArguments, safeContext, startedAt,
                        failure(traceId, ToolErrorCode.IDEMPOTENCY_KEY_REQUIRED,
                                "Idempotency key is required for tool: " + toolName, Map.of(), 0));
            }
            idempotencyScope = toolName + ":" + safeContext.principalId() + ":" + safeContext.idempotencyKey();
            ToolResult cached = idempotencyStore.find(idempotencyScope).orElse(null);
            if (cached != null) {
                return finish(traceId, toolName, safeArguments, safeContext, startedAt,
                        ToolResult.replay(traceId, cached));
            }
        }

        ToolResult result = executeWithRetries(traceId, registeredTool, safeArguments, safeContext);
        if (idempotencyScope != null && result.success()) {
            ToolResult stored = idempotencyStore.saveIfAbsent(idempotencyScope, result);
            if (stored != result) {
                result = ToolResult.replay(traceId, stored);
            }
        }
        return finish(traceId, toolName, safeArguments, safeContext, startedAt, result);
    }

    private ToolResult executeWithRetries(String traceId,
                                          ToolRegistry.RegisteredTool registeredTool,
                                          Map<String, Object> arguments,
                                          ToolInvocationContext context) {
        ToolDescriptor descriptor = registeredTool.descriptor();
        ToolError lastError = null;
        boolean lastFailureRetryable = false;

        for (int attempt = 1; attempt <= descriptor.maxAttempts(); attempt++) {
            Future<Object> future = executor.submit(() -> registeredTool.handler().execute(context, arguments));
            try {
                Object data = future.get(descriptor.timeout().toNanos(), TimeUnit.NANOSECONDS);
                return ToolResult.success(traceId, data, attempt);
            } catch (TimeoutException exception) {
                future.cancel(true);
                lastError = new ToolError(ToolErrorCode.TIMEOUT,
                        "Tool timed out after " + descriptor.timeout().toMillis() + " ms", Map.of());
                lastFailureRetryable = true;
            } catch (InterruptedException exception) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                return failure(traceId, ToolErrorCode.INTERRUPTED, "Tool invocation was interrupted", Map.of(), attempt);
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof ToolExecutionException toolException) {
                    lastError = new ToolError(toolException.code(), toolException.getMessage(), Map.of());
                    lastFailureRetryable = toolException.retryable();
                } else {
                    String message = cause == null || cause.getMessage() == null
                            ? "Tool execution failed"
                            : cause.getMessage();
                    lastError = new ToolError(ToolErrorCode.EXECUTION_FAILED, message, Map.of());
                    lastFailureRetryable = false;
                }
            }

            if (!lastFailureRetryable) {
                return ToolResult.failure(traceId, lastError, attempt);
            }
            if (attempt == descriptor.maxAttempts()) {
                if (descriptor.maxAttempts() == 1) {
                    return ToolResult.failure(traceId, lastError, attempt);
                }
                return failure(traceId, ToolErrorCode.RETRY_EXHAUSTED,
                        "Tool failed after " + attempt + " attempts",
                        Map.of("causeCode", lastError.code().name()), attempt);
            }
        }
        throw new IllegalStateException("Tool retry loop completed unexpectedly");
    }

    private ToolResult finish(String traceId,
                              String toolName,
                              Map<String, Object> arguments,
                              ToolInvocationContext context,
                              Instant startedAt,
                              ToolResult result) {
        ToolTraceStatus status = result.idempotencyReplay()
                ? ToolTraceStatus.REPLAYED
                : result.success() ? ToolTraceStatus.SUCCEEDED : ToolTraceStatus.FAILED;
        traceSink.record(new ToolTrace(
                traceId,
                context.invocationId(),
                toolName,
                context.principalId(),
                arguments,
                status,
                result.attempts(),
                result.error(),
                startedAt,
                Instant.now()
        ));
        return result;
    }

    private ToolResult failure(String traceId,
                               ToolErrorCode code,
                               String message,
                               Map<String, Object> details,
                               int attempts) {
        return ToolResult.failure(traceId, new ToolError(code, message, details), attempts);
    }

    private Map<String, Object> immutableArguments(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
    }

    @Override
    public void close() {
        if (ownsExecutor) {
            executor.shutdownNow();
        }
    }
}
