package com.recruit.agent.task.api;

import com.recruit.agent.task.domain.AgentStep;
import com.recruit.agent.task.domain.AgentTaskStatus;

import java.time.Instant;

public record AgentStepResponse(
        Long id,
        Long taskId,
        String stepName,
        AgentTaskStatus status,
        String message,
        Instant createdAt
) {

    public static AgentStepResponse from(AgentStep step) {
        return new AgentStepResponse(
                step.id(),
                step.taskId(),
                step.stepName(),
                step.status(),
                step.message(),
                step.createdAt()
        );
    }
}
