package com.recruit.agent.tool;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ToolInvocationContext(
        String invocationId,
        String principalId,
        Set<String> permissions,
        String idempotencyKey,
        Map<String, Object> attributes
) {
    public ToolInvocationContext {
        invocationId = invocationId == null || invocationId.isBlank()
                ? UUID.randomUUID().toString()
                : invocationId.trim();
        principalId = principalId == null || principalId.isBlank() ? "anonymous" : principalId.trim();
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        idempotencyKey = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static ToolInvocationContext anonymous() {
        return new ToolInvocationContext(null, null, Set.of(), null, Map.of());
    }
}
