package com.recruit.agent.task.checkpoint.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface TaskCheckpointMapper extends BaseMapper<TaskCheckpointPo> {
    @Select("""
            SELECT COUNT(*) FROM task_checkpoint
            WHERE task_id = #{taskId} AND checkpoint_key = #{checkpointKey}
            """)
    int countCompleted(@Param("taskId") Long taskId, @Param("checkpointKey") String checkpointKey);

    @Insert("""
            INSERT INTO task_checkpoint(task_id, checkpoint_key, completed_at)
            VALUES(#{taskId}, #{checkpointKey}, #{completedAt})
            ON DUPLICATE KEY UPDATE completed_at = completed_at
            """)
    int markCompleted(@Param("taskId") Long taskId,
                      @Param("checkpointKey") String checkpointKey,
                      @Param("completedAt") LocalDateTime completedAt);
}
