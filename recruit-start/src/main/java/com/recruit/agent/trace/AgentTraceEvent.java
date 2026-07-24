package com.recruit.agent.trace;

import com.recruit.agent.privacy.RecursiveSensitiveDataRedactor;
import com.recruit.agent.privacy.SensitiveDataRedactor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record AgentTraceEvent(
        String traceId,
        Long conversationId,
        String node,
        AgentTraceStatus status,
        Map<String, Object> details,
        Instant occurredAt
) {
    private static final SensitiveDataRedactor REDACTOR = new RecursiveSensitiveDataRedactor();

    public AgentTraceEvent {
        if (traceId == null || traceId.isBlank()) throw new IllegalArgumentException("traceId must not be blank");
        if (node == null || node.isBlank()) throw new IllegalArgumentException("node must not be blank");
        status = status == null ? AgentTraceStatus.SUCCEEDED : status;
        details = sanitize(details);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> sanitize(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return Map.of();
        }
        Map<?, ?> redacted = (Map<?, ?>) REDACTOR.redact(details);
        Map<String, Object> copy = new LinkedHashMap<>();
        redacted.forEach((key, value) -> copy.put(String.valueOf(key), value));
        return Map.copyOf(copy);
    }
}
