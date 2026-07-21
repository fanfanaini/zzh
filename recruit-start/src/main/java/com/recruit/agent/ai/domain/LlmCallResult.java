package com.recruit.agent.ai.domain;

public record LlmCallResult(
        String modelName,
        String responseBody,
        Integer promptTokens,
        Integer completionTokens,
        long latencyMillis
) {
}
