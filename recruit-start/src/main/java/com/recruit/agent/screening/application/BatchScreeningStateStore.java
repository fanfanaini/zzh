package com.recruit.agent.screening.application;

import java.util.Optional;

public interface BatchScreeningStateStore {
    Optional<Long> findTaskByIdempotencyKey(String key);
    Optional<Long> saveIdempotencyKey(String key, Long taskId);
    void saveJob(BatchScreeningJob job);
    Optional<BatchScreeningJob> findJob(Long taskId);
}
