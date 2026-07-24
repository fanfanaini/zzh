package com.recruit.agent.trace.api;

import com.recruit.agent.trace.AgentTraceEvent;
import com.recruit.agent.trace.AgentTraceStatus;
import com.recruit.agent.trace.TraceComponent;

import java.time.Instant;
import java.util.Map;

public record AgentTraceResponse(
        String traceId,
        Long conversationId,
        String node,
        TraceComponent component,
        String operation,
        String spanId,
        String parentSpanId,
        AgentTraceStatus status,
        Map<String, Object> details,
        Instant occurredAt
) {
    public static AgentTraceResponse from(AgentTraceEvent event) {
        return new AgentTraceResponse(event.traceId(), event.conversationId(), event.node(),
                component(event.details()), text(event.details(), "operation"), text(event.details(), "spanId"),
                text(event.details(), "parentSpanId"), event.status(),
                event.details(), event.occurredAt());
    }

    private static TraceComponent component(Map<String, Object> details) {
        String value = text(details, "component");
        if (value == null) return null;
        try {
            return TraceComponent.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String text(Map<String, Object> details, String key) {
        Object value = details.get(key);
        return value == null ? null : value.toString();
    }
}
