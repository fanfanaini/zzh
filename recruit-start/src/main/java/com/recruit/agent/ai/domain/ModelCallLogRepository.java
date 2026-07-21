package com.recruit.agent.ai.domain;

import java.util.Optional;
import java.util.List;

public interface ModelCallLogRepository {

    ModelCallLog save(ModelCallLog log);

    Optional<ModelCallLog> findById(Long id);

    List<ModelCallLog> findAll();

    long nextLogId();
}
