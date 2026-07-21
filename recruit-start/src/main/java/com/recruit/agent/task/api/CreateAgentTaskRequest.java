package com.recruit.agent.task.api;

import com.recruit.agent.task.domain.AgentTaskType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateAgentTaskRequest(
        @NotNull(message = "Agent 任务类型不能为空")
        AgentTaskType taskType,
        Long targetId,
        @Min(value = 1, message = "任务总数至少为 1")
        int totalCount
) {
}
