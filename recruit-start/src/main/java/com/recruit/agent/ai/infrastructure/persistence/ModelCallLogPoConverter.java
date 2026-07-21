package com.recruit.agent.ai.infrastructure.persistence;

import com.recruit.agent.ai.domain.ModelCallLog;
import com.recruit.agent.ai.domain.ModelCallStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ModelCallLogPoConverter {

    public ModelCallLogPo toPo(ModelCallLog log) {
        ModelCallLogPo po = new ModelCallLogPo();
        po.setId(log.id());
        po.setUseCase(log.useCase());
        po.setModelName(log.modelName());
        po.setPromptVersion(log.promptVersion());
        po.setSystemPrompt(log.systemPrompt());
        po.setUserPrompt(log.userPrompt());
        po.setResponseBody(log.responseBody());
        po.setPromptTokens(log.promptTokens());
        po.setCompletionTokens(log.completionTokens());
        po.setLatencyMillis(log.latencyMillis());
        po.setStatus(log.status().name());
        po.setErrorMessage(log.errorMessage());
        po.setCreatedAt(LocalDateTime.ofInstant(log.createdAt(), ZoneOffset.UTC));
        return po;
    }

    public ModelCallLog toDomain(ModelCallLogPo po) {
        return new ModelCallLog(
                po.getId(),
                po.getUseCase(),
                po.getModelName(),
                po.getPromptVersion(),
                po.getSystemPrompt(),
                po.getUserPrompt(),
                po.getResponseBody(),
                po.getPromptTokens(),
                po.getCompletionTokens(),
                po.getLatencyMillis(),
                ModelCallStatus.valueOf(po.getStatus()),
                po.getErrorMessage(),
                po.getCreatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}
