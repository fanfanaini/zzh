package com.recruit.agent.ops;

import java.time.Instant;
import java.util.Map;

public record AgentOpsAlert(
        String code,
        AlertSeverity severity,
        String title,
        String message,
        Map<String, Object> details,
        Instant triggeredAt
) {
    public AgentOpsAlert {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("alert code must not be blank");
        }
        severity = severity == null ? AlertSeverity.INFO : severity;
        title = title == null ? "" : title.trim();
        message = message == null ? "" : message.trim();
        details = details == null ? Map.of() : Map.copyOf(details);
        triggeredAt = triggeredAt == null ? Instant.now() : triggeredAt;
    }
}
