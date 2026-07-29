package com.recruit.agent.trace.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.trace.AgentTraceEvent;
import com.recruit.agent.trace.AgentTraceSink;
import com.recruit.agent.trace.AgentTraceStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Component
@Profile("mysql")
public class MyBatisAgentTraceSink implements AgentTraceSink {
    private static final TypeReference<Map<String, Object>> DETAILS_TYPE = new TypeReference<>() { };

    private final AgentTraceMapper mapper;
    private final ObjectMapper objectMapper;
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public MyBatisAgentTraceSink(AgentTraceMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(AgentTraceEvent event) {
        AgentTracePo po = new AgentTracePo();
        po.setId(idGenerator.nextId());
        po.setTraceId(event.traceId());
        po.setConversationId(event.conversationId());
        po.setNode(event.node());
        po.setStatus(event.status().name());
        po.setDetailsJson(writeDetails(event.details()));
        po.setOccurredAt(event.occurredAt().atOffset(ZoneOffset.UTC).toLocalDateTime());
        mapper.insert(po);
    }

    @Override
    public List<AgentTraceEvent> findByConversationId(Long conversationId) {
        return mapper.selectList(Wrappers.<AgentTracePo>lambdaQuery()
                        .eq(AgentTracePo::getConversationId, conversationId)
                        .orderByAsc(AgentTracePo::getOccurredAt)
                        .orderByAsc(AgentTracePo::getId))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<AgentTraceEvent> findByTraceId(String traceId) {
        return mapper.selectList(Wrappers.<AgentTracePo>lambdaQuery()
                        .eq(AgentTracePo::getTraceId, traceId)
                        .orderByAsc(AgentTracePo::getOccurredAt)
                        .orderByAsc(AgentTracePo::getId))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<AgentTraceEvent> findRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        return mapper.selectList(Wrappers.<AgentTracePo>lambdaQuery()
                        .orderByDesc(AgentTracePo::getOccurredAt)
                        .orderByDesc(AgentTracePo::getId)
                        .last("LIMIT " + safeLimit))
                .stream().map(this::toDomain).toList();
    }

    private AgentTraceEvent toDomain(AgentTracePo po) {
        return new AgentTraceEvent(po.getTraceId(), po.getConversationId(), po.getNode(),
                AgentTraceStatus.valueOf(po.getStatus()), readDetails(po.getDetailsJson()),
                po.getOccurredAt().toInstant(ZoneOffset.UTC));
    }

    private String writeDetails(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize agent trace details", exception);
        }
    }

    private Map<String, Object> readDetails(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, DETAILS_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize agent trace details", exception);
        }
    }
}
