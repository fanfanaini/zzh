package com.recruit.agent.screening.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.screening.application.BatchScreeningJob;
import com.recruit.agent.screening.application.BatchScreeningStateStore;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

public class RedisBatchScreeningStateStore implements BatchScreeningStateStore {
    private static final Duration RETENTION = Duration.ofDays(7);
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisBatchScreeningStateStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public Optional<Long> findTaskByIdempotencyKey(String key) {
        String value = redis.opsForValue().get(idempotencyKey(key));
        return value == null ? Optional.empty() : Optional.of(Long.valueOf(value));
    }

    public Optional<Long> saveIdempotencyKey(String key, Long taskId) {
        Boolean saved = redis.opsForValue().setIfAbsent(idempotencyKey(key), taskId.toString(), RETENTION);
        return Boolean.TRUE.equals(saved) ? Optional.empty() : findTaskByIdempotencyKey(key);
    }

    public void saveJob(BatchScreeningJob job) {
        try {
            redis.opsForValue().set(jobKey(job.taskId()), objectMapper.writeValueAsString(job), RETENTION);
        } catch (Exception exception) {
            throw new IllegalStateException("批量筛选任务写入 Redis 失败", exception);
        }
    }

    public Optional<BatchScreeningJob> findJob(Long taskId) {
        String value = redis.opsForValue().get(jobKey(taskId));
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, BatchScreeningJob.class));
        } catch (Exception exception) {
            throw new IllegalStateException("批量筛选任务从 Redis 恢复失败", exception);
        }
    }

    private String idempotencyKey(String key) { return "recruit:screening:idempotency:" + key; }
    private String jobKey(Long taskId) { return "recruit:screening:job:" + taskId; }
}
