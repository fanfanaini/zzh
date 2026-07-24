package com.recruit.agent.trace;

public record TraceContext(
        String traceId,
        Long conversationId,
        String parentSpanId
) {
    public TraceContext {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
        traceId = traceId.trim();
        parentSpanId = parentSpanId == null || parentSpanId.isBlank() ? null : parentSpanId.trim();
    }
}
