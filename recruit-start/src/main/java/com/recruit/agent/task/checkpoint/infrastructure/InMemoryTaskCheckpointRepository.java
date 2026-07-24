package com.recruit.agent.task.checkpoint.infrastructure;

import com.recruit.agent.task.checkpoint.domain.TaskCheckpointRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!mysql")
public class InMemoryTaskCheckpointRepository implements TaskCheckpointRepository {
    private final Set<String> completed = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isCompleted(Long taskId, String checkpointKey) {
        return completed.contains(scope(taskId, checkpointKey));
    }

    @Override
    public void markCompleted(Long taskId, String checkpointKey) {
        completed.add(scope(taskId, checkpointKey));
    }

    private String scope(Long taskId, String checkpointKey) {
        if (taskId == null || checkpointKey == null || checkpointKey.isBlank()) {
            throw new IllegalArgumentException("Task checkpoint identity must not be blank");
        }
        return taskId + ":" + checkpointKey.trim();
    }
}
