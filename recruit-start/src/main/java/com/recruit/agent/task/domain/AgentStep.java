package com.recruit.agent.task.domain;

import java.time.Instant;

public record AgentStep(
        Long id,
        Long taskId,
        String stepName,
        AgentTaskStatus status,
        String message,
        Instant createdAt
) {

    public AgentStep {
        if (id == null) {
            throw new IllegalArgumentException("Agent 步骤 ID 不能为空");
        }
        if (taskId == null) {
            throw new IllegalArgumentException("Agent 任务 ID 不能为空");
        }
        if (stepName == null || stepName.isBlank()) {
            throw new IllegalArgumentException("步骤名称不能为空");
        }
        stepName = stepName.trim();
        if (status == null) {
            status = AgentTaskStatus.PENDING;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
