package com.recruit.agent.task.api;

import com.recruit.agent.task.domain.AgentTask;
import com.recruit.agent.task.domain.AgentTaskStatus;
import com.recruit.agent.task.domain.AgentTaskType;

import java.time.Instant;

public record AgentTaskResponse(
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

    public static AgentTaskResponse from(AgentTask task) {
        return new AgentTaskResponse(
                task.id(),
                task.taskType(),
                task.status(),
                task.targetId(),
                task.totalCount(),
                task.successCount(),
                task.failureCount(),
                task.createdAt(),
                task.updatedAt()
        );
    }
}
