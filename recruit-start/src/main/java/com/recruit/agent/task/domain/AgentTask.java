package com.recruit.agent.task.domain;

import java.time.Instant;

public record AgentTask(
        Long id,
        AgentTaskType taskType,
        AgentTaskStatus status,
        Long targetId,
        int totalCount,
        int successCount,
        int failureCount,
        Instant createdAt,
        Instant updatedAt
) {

    public AgentTask {
        if (id == null) {
            throw new IllegalArgumentException("Agent 任务 ID 不能为空");
        }
        if (taskType == null) {
            throw new IllegalArgumentException("Agent 任务类型不能为空");
        }
        if (status == null) {
            status = AgentTaskStatus.PENDING;
        }
        if (totalCount < 0 || successCount < 0 || failureCount < 0) {
            throw new IllegalArgumentException("任务计数不能为负数");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    public AgentTask retry() {
        return new AgentTask(
                id,
                taskType,
                AgentTaskStatus.PENDING,
                targetId,
                totalCount,
                0,
                0,
                createdAt,
                Instant.now()
        );
    }

    public AgentTask start() {
        return withProgress(AgentTaskStatus.RUNNING, successCount, failureCount);
    }

    public AgentTask recordSuccess() {
        return withProgress(AgentTaskStatus.RUNNING, successCount + 1, failureCount);
    }

    public AgentTask recordFailure() {
        return withProgress(AgentTaskStatus.RUNNING, successCount, failureCount + 1);
    }

    public AgentTask finish() {
        AgentTaskStatus finalStatus = failureCount == 0 ? AgentTaskStatus.SUCCEEDED : AgentTaskStatus.FAILED;
        return withProgress(finalStatus, successCount, failureCount);
    }

    private AgentTask withProgress(AgentTaskStatus newStatus, int successes, int failures) {
        return new AgentTask(id, taskType, newStatus, targetId, totalCount, successes, failures,
                createdAt, Instant.now());
    }
}
