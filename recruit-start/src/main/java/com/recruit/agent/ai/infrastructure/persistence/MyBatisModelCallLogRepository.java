package com.recruit.agent.ai.infrastructure.persistence;

import com.recruit.agent.ai.domain.ModelCallLog;
import com.recruit.agent.ai.domain.ModelCallLogRepository;
import com.recruit.agent.common.id.TimestampIdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
@Profile("mysql")
public class MyBatisModelCallLogRepository implements ModelCallLogRepository {

    private final ModelCallLogMapper mapper;
    private final ModelCallLogPoConverter converter = new ModelCallLogPoConverter();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public MyBatisModelCallLogRepository(ModelCallLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ModelCallLog save(ModelCallLog log) {
        if (mapper.selectById(log.id()) == null) {
            mapper.insert(converter.toPo(log));
        } else {
            mapper.updateById(converter.toPo(log));
        }
        return log;
    }

    @Override
    public Optional<ModelCallLog> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(converter::toDomain);
    }

    @Override
    public List<ModelCallLog> findAll() {
        return mapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.<ModelCallLogPo>lambdaQuery()
                        .orderByDesc(ModelCallLogPo::getCreatedAt)).stream().map(converter::toDomain).toList();
    }

    @Override
    public long nextLogId() {
        return idGenerator.nextId();
    }
}
