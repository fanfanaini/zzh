package com.recruit.agent.task.checkpoint.domain;

public interface TaskCheckpointRepository {
    boolean isCompleted(Long taskId, String checkpointKey);

    void markCompleted(Long taskId, String checkpointKey);
}
