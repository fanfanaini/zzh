package com.recruit.agent.task.infrastructure.persistence;

import com.recruit.agent.task.domain.AgentStep;
import com.recruit.agent.task.domain.AgentTaskStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class AgentStepPoConverter {

    public AgentStepPo toPo(AgentStep step) {
        AgentStepPo po = new AgentStepPo();
        po.setId(step.id());
        po.setTaskId(step.taskId());
        po.setStepName(step.stepName());
        po.setStatus(step.status().name());
        po.setMessage(step.message());
        po.setCreatedAt(LocalDateTime.ofInstant(step.createdAt(), ZoneOffset.UTC));
        return po;
    }

    public AgentStep toDomain(AgentStepPo po) {
        return new AgentStep(
                po.getId(),
                po.getTaskId(),
                po.getStepName(),
                AgentTaskStatus.valueOf(po.getStatus()),
                po.getMessage(),
                po.getCreatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}
