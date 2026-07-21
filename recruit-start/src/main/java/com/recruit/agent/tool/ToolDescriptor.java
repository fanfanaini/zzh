package com.recruit.agent.tool;

import java.time.Duration;
import java.util.Set;

public record ToolDescriptor(
        String name,
        String description,
        ToolParameterContract parameterContract,
        Set<String> requiredPermissions,
        Duration timeout,
        int maxAttempts,
        boolean idempotent
) {
    private static final int MAX_ALLOWED_ATTEMPTS = 3;

    public ToolDescriptor {
        if (name == null || !name.matches("[a-z][a-z0-9_.-]{1,63}")) {
            throw new IllegalArgumentException("Tool name must use lowercase letters, digits, '.', '_' or '-'");
        }
        description = description == null ? "" : description.trim();
        parameterContract = parameterContract == null ? ToolParameterContract.none() : parameterContract;
        requiredPermissions = requiredPermissions == null ? Set.of() : Set.copyOf(requiredPermissions);
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Tool timeout must be positive");
        }
        if (maxAttempts < 1 || maxAttempts > MAX_ALLOWED_ATTEMPTS) {
            throw new IllegalArgumentException("Tool maxAttempts must be between 1 and " + MAX_ALLOWED_ATTEMPTS);
        }
    }
}
