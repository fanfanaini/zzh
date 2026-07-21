package com.recruit.agent.ai.domain;

public record LlmStructuredRequest<T>(
        String useCase,
        String promptVersion,
        String systemPrompt,
        String userPrompt,
        Class<T> resultType
) {
}
