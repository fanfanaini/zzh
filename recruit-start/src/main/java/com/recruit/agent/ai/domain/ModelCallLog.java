package com.recruit.agent.ai.domain;

import java.time.Instant;

public record ModelCallLog(
        Long id,
        String useCase,
        String modelName,
        String promptVersion,
        String systemPrompt,
        String userPrompt,
        String responseBody,
        Integer promptTokens,
        Integer completionTokens,
        Long latencyMillis,
        ModelCallStatus status,
        String errorMessage,
        Instant createdAt
) {

    public ModelCallLog {
        if (id == null) {
            throw new IllegalArgumentException("模型调用日志 ID 不能为空");
        }
        if (useCase == null || useCase.isBlank()) {
            throw new IllegalArgumentException("模型调用场景不能为空");
        }
        if (modelName == null || modelName.isBlank()) {
            modelName = "mock";
        }
        if (status == null) {
            status = ModelCallStatus.SUCCEEDED;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
