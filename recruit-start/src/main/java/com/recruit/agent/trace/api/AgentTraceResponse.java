package com.recruit.agent.trace.api;

import com.recruit.agent.trace.AgentTraceEvent;
import com.recruit.agent.trace.AgentTraceStatus;

import java.time.Instant;
import java.util.Map;

public record AgentTraceResponse(
        String traceId,
        Long conversationId,
        String node,
        AgentTraceStatus status,
        Map<String, Object> details,
        Instant occurredAt
) {
    public static AgentTraceResponse from(AgentTraceEvent event) {
        return new AgentTraceResponse(event.traceId(), event.conversationId(), event.node(), event.status(),
                event.details(), event.occurredAt());
    }
}
