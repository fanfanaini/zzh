package com.recruit.agent.tool;

import com.recruit.agent.trace.InMemoryToolTraceSink;
import com.recruit.agent.trace.ToolTraceStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolRuntimeTest {
    private final ToolRegistry registry = new ToolRegistry();
    private final InMemoryToolTraceSink traceSink = new InMemoryToolTraceSink();
    private final ToolRuntime runtime = new ToolRuntime(
            registry,
            new DefaultToolPermissionGuard(),
            new InMemoryToolIdempotencyStore(),
            traceSink
    );

    @AfterEach
    void closeRuntime() {
        runtime.close();
    }

    @Test
    void rejectsInvalidArgumentsBeforeCallingHandler() {
        AtomicInteger calls = new AtomicInteger();
        registry.register(descriptor("candidate.lookup", 1, false, Set.of()), (context, arguments) -> {
            calls.incrementAndGet();
            return "not reached";
        });

        ToolResult result = runtime.invoke(
                "candidate.lookup",
                Map.of("candidateId", "wrong-type", "extra", true),
                context(Set.of(), null)
        );

        assertThat(result.success()).isFalse();
        assertThat(result.error().code()).isEqualTo(ToolErrorCode.INVALID_ARGUMENTS);
        assertThat(result.error().details().get("violations").toString())
                .contains("candidateId", "extra");
        assertThat(calls).hasValue(0);
        assertThat(traceSink.findAll()).singleElement()
                .extracting(trace -> trace.status())
                .isEqualTo(ToolTraceStatus.FAILED);
    }

    @Test
    void deniesInvocationWhenPrincipalLacksPermission() {
        AtomicInteger calls = new AtomicInteger();
        registry.register(descriptor("candidate.lookup", 1, false, Set.of("candidate:read")),
                (context, arguments) -> calls.incrementAndGet());

        ToolResult result = runtime.invoke(
                "candidate.lookup",
                Map.of("candidateId", 42L),
                context(Set.of("job:read"), null)
        );

        assertThat(result.error().code()).isEqualTo(ToolErrorCode.PERMISSION_DENIED);
        assertThat(result.error().details().get("missingPermissions").toString())
                .contains("candidate:read");
        assertThat(calls).hasValue(0);
    }

    @Test
    void timesOutAndCancelsSlowHandler() {
        registry.register(descriptor("candidate.lookup", 1, false, Set.of(), Duration.ofMillis(30)),
                (context, arguments) -> {
                    Thread.sleep(2_000);
                    return "late";
                });

        ToolResult result = runtime.invoke(
                "candidate.lookup",
                Map.of("candidateId", 42L),
                context(Set.of(), null)
        );

        assertThat(result.success()).isFalse();
        assertThat(result.error().code()).isEqualTo(ToolErrorCode.TIMEOUT);
        assertThat(result.attempts()).isEqualTo(1);
    }

    @Test
    void retriesOnlyExplicitlyRetryableFailures() {
        AtomicInteger calls = new AtomicInteger();
        registry.register(descriptor("candidate.lookup", 3, false, Set.of()), (context, arguments) -> {
            if (calls.incrementAndGet() < 3) {
                throw new ToolExecutionException(ToolErrorCode.EXECUTION_FAILED, "temporary failure", true);
            }
            return Map.of("candidateId", arguments.get("candidateId"));
        });

        ToolResult result = runtime.invoke(
                "candidate.lookup",
                Map.of("candidateId", 42L),
                context(Set.of(), null)
        );

        assertThat(result.success()).isTrue();
        assertThat(result.attempts()).isEqualTo(3);
        assertThat(calls).hasValue(3);
        assertThat(traceSink.findAll().get(0).attempts()).isEqualTo(3);
    }

    @Test
    void returnsCachedSuccessForDuplicateIdempotencyKey() {
        AtomicInteger calls = new AtomicInteger();
        registry.register(descriptor("interview.schedule", 1, true, Set.of()),
                (context, arguments) -> Map.of("sequence", calls.incrementAndGet()));

        ToolResult first = runtime.invoke(
                "interview.schedule",
                Map.of("candidateId", 42L),
                context(Set.of(), "request-100")
        );
        ToolResult duplicate = runtime.invoke(
                "interview.schedule",
                Map.of("candidateId", 42L),
                context(Set.of(), "request-100")
        );

        assertThat(first.success()).isTrue();
        assertThat(duplicate.success()).isTrue();
        assertThat(duplicate.idempotencyReplay()).isTrue();
        assertThat(duplicate.attempts()).isZero();
        assertThat(duplicate.data()).isEqualTo(first.data());
        assertThat(calls).hasValue(1);
        assertThat(traceSink.findAll()).extracting(trace -> trace.status())
                .containsExactly(ToolTraceStatus.SUCCEEDED, ToolTraceStatus.REPLAYED);
    }

    @Test
    void executesConcurrentInvocationsWithTheSameScopeOnlyOnce() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch handlerStarted = new CountDownLatch(1);
        CountDownLatch releaseHandler = new CountDownLatch(1);
        registry.register(descriptor("interview.schedule", 1, true, Set.of()), (context, arguments) -> {
            calls.incrementAndGet();
            handlerStarted.countDown();
            releaseHandler.await(1, TimeUnit.SECONDS);
            return Map.of("interviewId", 91L);
        });

        ExecutorService callers = Executors.newFixedThreadPool(2);
        try {
            Future<ToolResult> first = callers.submit(() -> runtime.invoke(
                    "interview.schedule", Map.of("candidateId", 42L), context(Set.of(), "request-100")));
            assertThat(handlerStarted.await(1, TimeUnit.SECONDS)).isTrue();
            Future<ToolResult> duplicate = callers.submit(() -> runtime.invoke(
                    "interview.schedule", Map.of("candidateId", 42L), context(Set.of(), "request-100")));

            Thread.sleep(50);
            releaseHandler.countDown();

            assertThat(List.of(first.get(1, TimeUnit.SECONDS), duplicate.get(1, TimeUnit.SECONDS)))
                    .extracting(ToolResult::success)
                    .containsOnly(true);
            assertThat(List.of(first.get(), duplicate.get()))
                    .extracting(ToolResult::idempotencyReplay)
                    .containsExactlyInAnyOrder(false, true);
            assertThat(calls).hasValue(1);
        } finally {
            releaseHandler.countDown();
            callers.shutdownNow();
        }
    }

    @Test
    void scopesIdempotencyByTenantFromInvocationAttributes() {
        AtomicInteger calls = new AtomicInteger();
        registry.register(descriptor("interview.schedule", 1, true, Set.of()),
                (context, arguments) -> calls.incrementAndGet());

        ToolResult tenantOne = runtime.invoke(
                "interview.schedule",
                Map.of("candidateId", 42L),
                context(Set.of(), "request-100", Map.of("tenantId", "tenant-1"))
        );
        ToolResult tenantTwo = runtime.invoke(
                "interview.schedule",
                Map.of("candidateId", 42L),
                context(Set.of(), "request-100", Map.of("tenantId", "tenant-2"))
        );

        assertThat(tenantOne.data()).isEqualTo(1);
        assertThat(tenantTwo.data()).isEqualTo(2);
        assertThat(calls).hasValue(2);
    }

    @Test
    void requiresIdempotencyKeyForSideEffectingTool() {
        registry.register(descriptor("interview.schedule", 1, true, Set.of()),
                (context, arguments) -> "not reached");

        ToolResult result = runtime.invoke(
                "interview.schedule",
                Map.of("candidateId", 42L),
                context(Set.of(), null)
        );

        assertThat(result.error().code()).isEqualTo(ToolErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    @Test
    void rejectsDuplicateToolRegistration() {
        ToolDescriptor descriptor = descriptor("candidate.lookup", 1, false, Set.of());
        registry.register(descriptor, (context, arguments) -> "first");

        assertThatThrownBy(() -> registry.register(descriptor, (context, arguments) -> "second"))
                .isInstanceOfSatisfying(ToolRegistryException.class,
                        exception -> assertThat(exception.code()).isEqualTo(ToolErrorCode.DUPLICATE_TOOL));
    }

    @Test
    void capsRetryCountAtThreeAttempts() {
        assertThatThrownBy(() -> descriptor("candidate.lookup", 4, false, Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 3");
    }

    private ToolDescriptor descriptor(String name,
                                      int maxAttempts,
                                      boolean idempotent,
                                      Set<String> permissions) {
        return descriptor(name, maxAttempts, idempotent, permissions, Duration.ofSeconds(1));
    }

    private ToolDescriptor descriptor(String name,
                                      int maxAttempts,
                                      boolean idempotent,
                                      Set<String> permissions,
                                      Duration timeout) {
        return new ToolDescriptor(
                name,
                "Test tool",
                new ToolParameterContract(List.of(
                        new ToolParameter("candidateId", ToolParameterType.LONG, true, "Candidate ID")
                ), true),
                permissions,
                timeout,
                maxAttempts,
                idempotent
        );
    }

    private ToolInvocationContext context(Set<String> permissions, String idempotencyKey) {
        return context(permissions, idempotencyKey, Map.of("conversationId", "conversation-9"));
    }

    private ToolInvocationContext context(Set<String> permissions,
                                          String idempotencyKey,
                                          Map<String, Object> attributes) {
        return new ToolInvocationContext(
                null,
                "hr-7",
                permissions,
                idempotencyKey,
                attributes
        );
    }
}
