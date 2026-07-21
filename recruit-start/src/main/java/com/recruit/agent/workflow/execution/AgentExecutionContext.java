package com.recruit.agent.workflow.execution;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public record AgentExecutionContext(
        String traceId,
        Long conversationId,
        String tenantId,
        String taskId,
        String principalId,
        Set<String> roles,
        Set<String> permissions,
        String promptVersion,
        String modelVersion,
        Map<String, String> attributes
) implements Serializable {
    public AgentExecutionContext {
        if (traceId == null || traceId.isBlank()) throw new IllegalArgumentException("traceId must not be blank");
        if (conversationId == null) throw new IllegalArgumentException("conversationId must not be null");
        tenantId = tenantId == null || tenantId.isBlank() ? "default" : tenantId.trim();
        principalId = principalId == null || principalId.isBlank() ? "anonymous" : principalId.trim();
        taskId = taskId == null || taskId.isBlank() ? null : taskId.trim();
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        promptVersion = normalize(promptVersion, "runtime-resolved");
        modelVersion = normalize(modelVersion, "unknown");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
