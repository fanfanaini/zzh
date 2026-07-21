package com.recruit.agent.task.infrastructure;

import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.task.domain.AgentStep;
import com.recruit.agent.task.domain.AgentStepRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("local")
public class InMemoryAgentStepRepository implements AgentStepRepository {

    private final Map<Long, AgentStep> steps = new ConcurrentHashMap<>();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    @Override
    public AgentStep save(AgentStep step) {
        steps.put(step.id(), step);
        return step;
    }

    @Override
    public List<AgentStep> findByTaskId(Long taskId) {
        return steps.values().stream()
                .filter(step -> step.taskId().equals(taskId))
                .sorted(Comparator.comparing(AgentStep::createdAt))
                .toList();
    }

    @Override
    public long nextStepId() {
        return idGenerator.nextId();
    }
}
