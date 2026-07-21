package com.recruit.agent.task.infrastructure.persistence;

import com.recruit.agent.task.domain.AgentTask;
import com.recruit.agent.task.domain.AgentTaskStatus;
import com.recruit.agent.task.domain.AgentTaskType;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class AgentTaskPoConverter {

    public AgentTaskPo toPo(AgentTask task) {
        AgentTaskPo po = new AgentTaskPo();
        po.setId(task.id());
        po.setTaskType(task.taskType().name());
        po.setStatus(task.status().name());
        po.setTargetId(task.targetId());
        po.setTotalCount(task.totalCount());
        po.setSuccessCount(task.successCount());
        po.setFailureCount(task.failureCount());
        po.setCreatedAt(LocalDateTime.ofInstant(task.createdAt(), ZoneOffset.UTC));
        po.setUpdatedAt(LocalDateTime.ofInstant(task.updatedAt(), ZoneOffset.UTC));
        return po;
    }

    public AgentTask toDomain(AgentTaskPo po) {
        return new AgentTask(
                po.getId(),
                AgentTaskType.valueOf(po.getTaskType()),
                AgentTaskStatus.valueOf(po.getStatus()),
                po.getTargetId(),
                po.getTotalCount(),
                po.getSuccessCount(),
                po.getFailureCount(),
                po.getCreatedAt().toInstant(ZoneOffset.UTC),
                po.getUpdatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}
