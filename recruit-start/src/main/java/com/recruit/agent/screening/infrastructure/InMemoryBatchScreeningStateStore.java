package com.recruit.agent.screening.infrastructure;

import com.recruit.agent.screening.application.BatchScreeningJob;
import com.recruit.agent.screening.application.BatchScreeningStateStore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBatchScreeningStateStore implements BatchScreeningStateStore {
    private final Map<String, Long> idempotencyKeys = new ConcurrentHashMap<>();
    private final Map<Long, BatchScreeningJob> jobs = new ConcurrentHashMap<>();

    public Optional<Long> findTaskByIdempotencyKey(String key) {
        return Optional.ofNullable(idempotencyKeys.get(key));
    }
    public Optional<Long> saveIdempotencyKey(String key, Long taskId) {
        return Optional.ofNullable(idempotencyKeys.putIfAbsent(key, taskId));
    }
    public void saveJob(BatchScreeningJob job) { jobs.put(job.taskId(), job); }
    public Optional<BatchScreeningJob> findJob(Long taskId) { return Optional.ofNullable(jobs.get(taskId)); }
}
