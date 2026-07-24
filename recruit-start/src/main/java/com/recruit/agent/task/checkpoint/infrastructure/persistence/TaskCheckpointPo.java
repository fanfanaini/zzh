package com.recruit.agent.task.checkpoint.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("task_checkpoint")
public class TaskCheckpointPo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String checkpointKey;
    private LocalDateTime completedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public String getCheckpointKey() { return checkpointKey; }
    public void setCheckpointKey(String checkpointKey) { this.checkpointKey = checkpointKey; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
