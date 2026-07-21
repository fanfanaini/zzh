package com.recruit.agent.workflow.execution;

import com.recruit.agent.trace.AgentTraceEvent;
import com.recruit.agent.trace.AgentTraceSink;
import com.recruit.agent.trace.AgentTraceStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class AgentNodeLifecycleInterceptor {
    private final AgentTraceSink traceSink;

    public AgentNodeLifecycleInterceptor(AgentTraceSink traceSink) {
        this.traceSink = traceSink;
    }

    public Map<String, Object> execute(AgentExecutionContext context, String node,
                                       Supplier<AgentNodeResult> action) {
        long startedNanos = System.nanoTime();
        traceSink.record(event(context, node, AgentTraceStatus.STARTED, baseDetails(context, 1)));
        try {
            AgentNodeResult result = action.get();
            Map<String, Object> details = baseDetails(context, 1);
            details.putAll(result.traceDetails());
            details.put("durationMillis", elapsedMillis(startedNanos));
            traceSink.record(event(context, node, AgentTraceStatus.SUCCEEDED, details));
            return result.updates();
        } catch (RuntimeException exception) {
            Map<String, Object> details = baseDetails(context, 1);
            details.put("durationMillis", elapsedMillis(startedNanos));
            details.put("errorCode", exception.getClass().getSimpleName());
            details.put("error", safeMessage(exception));
            traceSink.record(event(context, node, AgentTraceStatus.FAILED, details));
            throw exception;
        }
    }

    private AgentTraceEvent event(AgentExecutionContext context, String node, AgentTraceStatus status,
                                  Map<String, Object> details) {
        return new AgentTraceEvent(context.traceId(), context.conversationId(), node, status, details, Instant.now());
    }

    private Map<String, Object> baseDetails(AgentExecutionContext context, int attempt) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("attempt", attempt);
        details.put("tenantId", context.tenantId());
        details.put("principalId", context.principalId());
        details.put("roles", context.roles());
        details.put("promptVersion", context.promptVersion());
        details.put("modelVersion", context.modelVersion());
        if (context.taskId() != null) details.put("taskId", context.taskId());
        return details;
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
