package com.recruit.agent.ai.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.ai.domain.LlmCallResult;
import com.recruit.agent.ai.domain.LlmFailureCategory;
import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.ai.domain.LlmStructuredRequest;
import com.recruit.agent.ai.domain.ModelCallLog;
import com.recruit.agent.ai.domain.ModelCallLogRepository;
import com.recruit.agent.ai.domain.ModelCallStatus;
import com.recruit.agent.ai.domain.RawLlmClient;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.LongConsumer;

public class DefaultLlmService implements LlmService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RawLlmClient rawClient;
    private final ModelCallLogRepository logRepository;
    private final LlmFailureClassifier failureClassifier = new LlmFailureClassifier();
    private final int maxAttempts;
    private final long initialBackoffMillis;
    private final long maximumBackoffMillis;
    private final LongConsumer sleeper;
    private final DoubleSupplier jitter;

    public DefaultLlmService(RawLlmClient rawClient, ModelCallLogRepository logRepository) {
        this(rawClient, logRepository, 3, 200, 2_000, DefaultLlmService::sleep,
                () -> ThreadLocalRandom.current().nextDouble());
    }

    public DefaultLlmService(RawLlmClient rawClient,
                             ModelCallLogRepository logRepository,
                             int maxAttempts,
                             long initialBackoffMillis,
                             long maximumBackoffMillis,
                             LongConsumer sleeper,
                             DoubleSupplier jitter) {
        if (maxAttempts < 1 || initialBackoffMillis < 0 || maximumBackoffMillis < initialBackoffMillis) {
            throw new IllegalArgumentException("Invalid LLM retry configuration");
        }
        this.rawClient = rawClient;
        this.logRepository = logRepository;
        this.maxAttempts = maxAttempts;
        this.initialBackoffMillis = initialBackoffMillis;
        this.maximumBackoffMillis = maximumBackoffMillis;
        this.sleeper = sleeper;
        this.jitter = jitter;
    }

    @Override
    public <T> T structuredChat(LlmStructuredRequest<T> request) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                LlmCallResult result = rawClient.structuredChat(request.systemPrompt(), request.userPrompt());
                T parsed = objectMapper.readValue(result.responseBody(), request.resultType());
                saveLog(request, result, ModelCallStatus.SUCCEEDED, null);
                return parsed;
            } catch (Exception exception) {
                LlmFailureCategory category = failureClassifier.classify(exception);
                saveLog(request, null, ModelCallStatus.FAILED,
                        "[" + category.name() + "] attempt=" + attempt + " " + safeMessage(exception));
                if (!category.retryable() || attempt == maxAttempts) {
                    throw new IllegalStateException(
                            "Structured LLM call failed [" + category.name() + "] after "
                                    + attempt + " attempt(s)",
                            exception);
                }
                sleeper.accept(backoffMillis(attempt));
            }
        }
        throw new IllegalStateException("LLM retry loop completed unexpectedly");
    }

    private <T> void saveLog(LlmStructuredRequest<T> request,
                             LlmCallResult result,
                             ModelCallStatus status,
                             String error) {
        logRepository.save(new ModelCallLog(
                logRepository.nextLogId(),
                request.useCase(),
                result == null ? rawClient.modelName() : result.modelName(),
                request.promptVersion(),
                request.systemPrompt(),
                request.userPrompt(),
                result == null ? null : result.responseBody(),
                result == null ? null : result.promptTokens(),
                result == null ? null : result.completionTokens(),
                result == null ? null : result.latencyMillis(),
                status,
                error,
                Instant.now()
        ));
    }

    private long backoffMillis(int failedAttempt) {
        long multiplier = 1L << Math.min(failedAttempt - 1, 30);
        long exponential;
        try {
            exponential = Math.multiplyExact(initialBackoffMillis, multiplier);
        } catch (ArithmeticException exception) {
            exponential = maximumBackoffMillis;
        }
        long capped = Math.min(maximumBackoffMillis, exponential);
        double jitterValue = Math.max(0, Math.min(1, jitter.getAsDouble()));
        return Math.min(maximumBackoffMillis, Math.round(capped * (1 + jitterValue * 0.25)));
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM retry backoff was interrupted", exception);
        }
    }
}
