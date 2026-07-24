package com.recruit.agent.task.checkpoint.infrastructure.persistence;

import com.recruit.agent.task.checkpoint.domain.TaskCheckpointRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@Profile("mysql")
public class MyBatisTaskCheckpointRepository implements TaskCheckpointRepository {
    private final TaskCheckpointMapper mapper;

    public MyBatisTaskCheckpointRepository(TaskCheckpointMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean isCompleted(Long taskId, String checkpointKey) {
        return mapper.countCompleted(taskId, checkpointKey) > 0;
    }

    @Override
    public void markCompleted(Long taskId, String checkpointKey) {
        mapper.markCompleted(taskId, checkpointKey, LocalDateTime.now());
    }
}
