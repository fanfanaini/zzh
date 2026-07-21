package com.recruit.agent.task.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentStepMapper extends BaseMapper<AgentStepPo> {
}
