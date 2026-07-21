package com.recruit.agent.ai.infrastructure;

import com.recruit.agent.ai.domain.ModelCallLog;
import com.recruit.agent.ai.domain.ModelCallLogRepository;
import com.recruit.agent.common.id.TimestampIdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

@Repository
@Profile("!mysql")
public class InMemoryModelCallLogRepository implements ModelCallLogRepository {

    private final Map<Long, ModelCallLog> logs = new ConcurrentHashMap<>();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    @Override
    public ModelCallLog save(ModelCallLog log) {
        logs.put(log.id(), log);
        return log;
    }

    @Override
    public Optional<ModelCallLog> findById(Long id) {
        return Optional.ofNullable(logs.get(id));
    }

    @Override
    public List<ModelCallLog> findAll() {
        return logs.values().stream().sorted(java.util.Comparator.comparing(ModelCallLog::createdAt).reversed()).toList();
    }

    @Override
    public long nextLogId() {
        return idGenerator.nextId();
    }
}
