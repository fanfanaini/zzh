package com.recruit.agent.trace;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AgentTraceToolTraceSink implements ToolTraceSink {
    private final UnifiedTraceRecorder recorder;

    public AgentTraceToolTraceSink(UnifiedTraceRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void record(ToolTrace trace) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("toolName", trace.toolName());
        details.put("principalId", trace.principalId());
        details.put("argumentKeys", trace.arguments().keySet());
        details.put("attempts", trace.attempts());
        details.put("durationMillis", durationMillis(trace));
        if (trace.error() != null) {
            details.put("errorCode", trace.error().code().name());
            details.put("error", trace.error().message());
        }
        recorder.recordExternal(
                context(trace),
                TraceComponent.TOOL,
                trace.toolName() == null || trace.toolName().isBlank() ? "unknown" : trace.toolName(),
                trace.traceId(),
                status(trace.status()),
                details
        );
    }

    private TraceContext context(ToolTrace trace) {
        TraceContext current = TraceContextHolder.current().orElse(null);
        Long conversationId = trace.conversationId();
        if (conversationId == null && current != null) {
            conversationId = current.conversationId();
        }
        String rootTraceId = trace.invocationId() == null || trace.invocationId().isBlank()
                ? trace.traceId()
                : trace.invocationId();
        return new TraceContext(rootTraceId, conversationId, current == null ? null : current.parentSpanId());
    }

    private AgentTraceStatus status(ToolTraceStatus status) {
        return switch (status) {
            case SUCCEEDED, REPLAYED -> AgentTraceStatus.SUCCEEDED;
            case FAILED -> AgentTraceStatus.FAILED;
        };
    }

    private long durationMillis(ToolTrace trace) {
        if (trace.startedAt() == null || trace.finishedAt() == null) {
            return 0;
        }
        return Math.max(0, java.time.Duration.between(trace.startedAt(), trace.finishedAt()).toMillis());
    }

}
