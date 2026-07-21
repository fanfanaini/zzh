package com.recruit.agent.task.infrastructure.persistence;

import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.task.domain.AgentTask;
import com.recruit.agent.task.domain.AgentTaskRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
@Profile("mysql")
public class MyBatisAgentTaskRepository implements AgentTaskRepository {

    private final AgentTaskMapper mapper;
    private final AgentTaskPoConverter converter = new AgentTaskPoConverter();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public MyBatisAgentTaskRepository(AgentTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public AgentTask save(AgentTask task) {
        if (mapper.selectById(task.id()) == null) {
            mapper.insert(converter.toPo(task));
        } else {
            mapper.updateById(converter.toPo(task));
        }
        return task;
    }

    @Override
    public Optional<AgentTask> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(converter::toDomain);
    }

    @Override
    public List<AgentTask> findAll() {
        return mapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AgentTaskPo>()
                        .orderByDesc("created_at"))
                .stream().map(converter::toDomain).toList();
    }

    @Override
    public long nextTaskId() {
        return idGenerator.nextId();
    }
}
