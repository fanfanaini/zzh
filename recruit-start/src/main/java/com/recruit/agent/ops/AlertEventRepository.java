package com.recruit.agent.ops;

import java.util.List;
import java.util.Optional;

public interface AlertEventRepository {

    AlertEvent save(AlertEvent event);

    Optional<AlertEvent> findById(Long id);

    Optional<AlertEvent> findOpenByFingerprint(String fingerprint);

    List<AlertEvent> findOpen();

    List<AlertEvent> findAll();

    long nextId();
}
