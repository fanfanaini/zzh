package com.recruit.agent.ops;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAlertEventRepository implements AlertEventRepository {

    private final AtomicLong idGenerator = new AtomicLong(5000);
    private final ConcurrentHashMap<Long, AlertEvent> storage = new ConcurrentHashMap<>();

    @Override
    public AlertEvent save(AlertEvent event) {
        storage.put(event.id(), event);
        return event;
    }

    @Override
    public Optional<AlertEvent> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<AlertEvent> findOpenByFingerprint(String fingerprint) {
        return storage.values().stream()
                .filter(event -> event.fingerprint().equals(fingerprint))
                .filter(event -> event.status() != AlertEventStatus.RESOLVED)
                .findFirst();
    }

    @Override
    public List<AlertEvent> findOpen() {
        return storage.values().stream()
                .filter(event -> event.status() != AlertEventStatus.RESOLVED)
                .sorted(Comparator.comparing(AlertEvent::updatedAt).reversed())
                .toList();
    }

    @Override
    public List<AlertEvent> findAll() {
        ArrayList<AlertEvent> events = new ArrayList<>(storage.values());
        events.sort(Comparator.comparing(AlertEvent::updatedAt).reversed());
        return List.copyOf(events);
    }

    @Override
    public long nextId() {
        return idGenerator.incrementAndGet();
    }
}
