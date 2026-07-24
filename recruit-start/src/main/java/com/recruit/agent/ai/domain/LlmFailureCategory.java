package com.recruit.agent.ai.domain;

public enum LlmFailureCategory {
    CONTEXT_OVERFLOW,
    RATE_LIMIT,
    PROVIDER_OVERLOADED,
    TIMEOUT,
    INVALID_REQUEST,
    AUTHENTICATION,
    UNKNOWN;

    public boolean retryable() {
        return this == RATE_LIMIT || this == PROVIDER_OVERLOADED || this == TIMEOUT;
    }
}
