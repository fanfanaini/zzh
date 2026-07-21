package com.recruit.agent.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.ai.domain.LlmCallResult;
import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.ai.domain.LlmStructuredRequest;
import com.recruit.agent.ai.domain.ModelCallLog;
import com.recruit.agent.ai.domain.ModelCallLogRepository;
import com.recruit.agent.ai.domain.ModelCallStatus;
import com.recruit.agent.ai.domain.RawLlmClient;

import java.time.Instant;

public class DefaultLlmService implements LlmService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RawLlmClient rawClient;
    private final ModelCallLogRepository logRepository;

    public DefaultLlmService(RawLlmClient rawClient, ModelCallLogRepository logRepository) {
        this.rawClient = rawClient;
        this.logRepository = logRepository;
    }

    @Override
    public <T> T structuredChat(LlmStructuredRequest<T> request) {
        try {
            LlmCallResult result = rawClient.structuredChat(request.systemPrompt(), request.userPrompt());
            logRepository.save(new ModelCallLog(
                    logRepository.nextLogId(),
                    request.useCase(),
                    result.modelName(),
                    request.promptVersion(),
                    request.systemPrompt(),
                    request.userPrompt(),
                    result.responseBody(),
                    result.promptTokens(),
                    result.completionTokens(),
                    result.latencyMillis(),
                    ModelCallStatus.SUCCEEDED,
                    null,
                    Instant.now()
            ));
            return objectMapper.readValue(result.responseBody(), request.resultType());
        } catch (Exception exception) {
            logRepository.save(new ModelCallLog(
                    logRepository.nextLogId(),
                    request.useCase(),
                    rawClient.modelName(),
                    request.promptVersion(),
                    request.systemPrompt(),
                    request.userPrompt(),
                    null,
                    null,
                    null,
                    null,
                    ModelCallStatus.FAILED,
                    exception.getMessage(),
                    Instant.now()
            ));
            throw new IllegalStateException("结构化 LLM 调用失败", exception);
        }
    }
}
