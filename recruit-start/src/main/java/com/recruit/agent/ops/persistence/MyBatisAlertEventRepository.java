package com.recruit.agent.ops.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.ops.AlertEvent;
import com.recruit.agent.ops.AlertEventRepository;
import com.recruit.agent.ops.AlertEventStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisAlertEventRepository implements AlertEventRepository {

    private final AlertEventMapper mapper;
    private final AlertEventPoConverter converter;
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public MyBatisAlertEventRepository(AlertEventMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.converter = new AlertEventPoConverter(objectMapper);
    }

    @Override
    public AlertEvent save(AlertEvent event) {
        AlertEventPo po = converter.toPo(event);
        if (mapper.selectById(event.id()) == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return event;
    }

    @Override
    public Optional<AlertEvent> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(converter::toDomain);
    }

    @Override
    public Optional<AlertEvent> findOpenByFingerprint(String fingerprint) {
        return mapper.selectList(Wrappers.<AlertEventPo>lambdaQuery()
                        .eq(AlertEventPo::getFingerprint, fingerprint)
                        .ne(AlertEventPo::getStatus, AlertEventStatus.RESOLVED.name())
                        .last("limit 1"))
                .stream()
                .findFirst()
                .map(converter::toDomain);
    }

    @Override
    public List<AlertEvent> findOpen() {
        return mapper.selectList(Wrappers.<AlertEventPo>lambdaQuery()
                        .ne(AlertEventPo::getStatus, AlertEventStatus.RESOLVED.name())
                        .orderByDesc(AlertEventPo::getUpdatedAt))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public List<AlertEvent> findAll() {
        return mapper.selectList(Wrappers.<AlertEventPo>lambdaQuery()
                        .orderByDesc(AlertEventPo::getUpdatedAt))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public long nextId() {
        return idGenerator.nextId();
    }
}
