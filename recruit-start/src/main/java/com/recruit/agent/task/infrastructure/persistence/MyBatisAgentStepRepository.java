package com.recruit.agent.task.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.task.domain.AgentStep;
import com.recruit.agent.task.domain.AgentStepRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("mysql")
public class MyBatisAgentStepRepository implements AgentStepRepository {

    private final AgentStepMapper mapper;
    private final AgentStepPoConverter converter = new AgentStepPoConverter();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public MyBatisAgentStepRepository(AgentStepMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AgentStep save(AgentStep step) {
        if (mapper.selectById(step.id()) == null) {
            mapper.insert(converter.toPo(step));
        } else {
            mapper.updateById(converter.toPo(step));
        }
        return step;
    }

    @Override
    public List<AgentStep> findByTaskId(Long taskId) {
        return mapper.selectList(Wrappers.<AgentStepPo>lambdaQuery()
                        .eq(AgentStepPo::getTaskId, taskId)
                        .orderByAsc(AgentStepPo::getCreatedAt))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public long nextStepId() {
        return idGenerator.nextId();
    }
}
