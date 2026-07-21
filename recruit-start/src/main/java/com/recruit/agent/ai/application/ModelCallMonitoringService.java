package com.recruit.agent.ai.application;

import com.recruit.agent.ai.domain.ModelCallLog;
import com.recruit.agent.ai.domain.ModelCallLogRepository;
import com.recruit.agent.ai.domain.ModelCallStatus;

import java.util.List;

public class ModelCallMonitoringService {
    private final ModelCallLogRepository repository;
    public ModelCallMonitoringService(ModelCallLogRepository repository) { this.repository = repository; }

    public List<ModelCallLog> list() { return repository.findAll(); }

    public ModelCallSummary summary() {
        List<ModelCallLog> logs = repository.findAll();
        long succeeded = logs.stream().filter(log -> log.status() == ModelCallStatus.SUCCEEDED).count();
        long promptTokens = logs.stream().map(ModelCallLog::promptTokens).filter(java.util.Objects::nonNull).mapToLong(Integer::longValue).sum();
        long completionTokens = logs.stream().map(ModelCallLog::completionTokens).filter(java.util.Objects::nonNull).mapToLong(Integer::longValue).sum();
        double averageLatency = logs.stream().map(ModelCallLog::latencyMillis).filter(java.util.Objects::nonNull)
                .mapToLong(Long::longValue).average().orElse(0);
        return new ModelCallSummary(logs.size(), succeeded, logs.size() - succeeded, promptTokens, completionTokens, averageLatency);
    }

    public record ModelCallSummary(long totalCalls, long succeededCalls, long failedCalls,
                                   long promptTokens, long completionTokens, double averageLatencyMillis) {}
}
