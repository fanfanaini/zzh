package com.recruit.agent.task.domain;

import java.util.Optional;
import java.util.List;

public interface AgentTaskRepository {

    AgentTask save(AgentTask task);

    Optional<AgentTask> findById(Long id);

    List<AgentTask> findAll();

    long nextTaskId();
}
