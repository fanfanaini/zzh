package com.recruit.agent.task.application;

import com.recruit.agent.task.domain.AgentTaskType;

public record CreateAgentTaskCommand(
        AgentTaskType taskType,
        Long targetId,
        int totalCount
) {
}
