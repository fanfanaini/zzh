package com.recruit.agent.jobapplication.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobApplicationHistoryMapper extends BaseMapper<JobApplicationHistoryPo> {
}
