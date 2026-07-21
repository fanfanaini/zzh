package com.recruit.agent.trace;

import java.time.Instant;
import java.util.Map;

public record AgentTraceEvent(
        String traceId,
        Long conversationId,
        String node,
        AgentTraceStatus status,
        Map<String, Object> details,
        Instant occurredAt
) {
    public AgentTraceEvent {
        if (traceId == null || traceId.isBlank()) throw new IllegalArgumentException("traceId must not be blank");
        if (node == null || node.isBlank()) throw new IllegalArgumentException("node must not be blank");
        status = status == null ? AgentTraceStatus.SUCCEEDED : status;
        details = details == null ? Map.of() : Map.copyOf(details);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
}
