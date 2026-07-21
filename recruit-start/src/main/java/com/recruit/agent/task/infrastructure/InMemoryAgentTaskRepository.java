package com.recruit.agent.task.infrastructure;

import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.task.domain.AgentTask;
import com.recruit.agent.task.domain.AgentTaskRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

@Repository
@Profile("local")
public class InMemoryAgentTaskRepository implements AgentTaskRepository {

    private final Map<Long, AgentTask> tasks = new ConcurrentHashMap<>();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    @Override
    public AgentTask save(AgentTask task) {
        tasks.put(task.id(), task);
        return task;
    }

    @Override
    public Optional<AgentTask> findById(Long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<AgentTask> findAll() {
        return tasks.values().stream().sorted(java.util.Comparator.comparing(AgentTask::createdAt).reversed()).toList();
    }

    @Override
    public long nextTaskId() {
        return idGenerator.nextId();
    }
}
