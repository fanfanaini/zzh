package com.recruit.agent.task.domain;

import java.util.List;

public interface AgentStepRepository {

    AgentStep save(AgentStep step);

    List<AgentStep> findByTaskId(Long taskId);

    long nextStepId();
}
