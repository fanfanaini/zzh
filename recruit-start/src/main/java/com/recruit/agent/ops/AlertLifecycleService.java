package com.recruit.agent.ops;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

public class AlertLifecycleService {

    private final AgentOpsService opsService;
    private final AlertEventRepository repository;
    private final AlertNotifier notifier;
    private final AgentOpsProperties properties;

    public AlertLifecycleService(AgentOpsService opsService, AlertEventRepository repository,
                                 AlertNotifier notifier, AgentOpsProperties properties) {
        this.opsService = opsService;
        this.repository = repository;
        this.notifier = notifier;
        this.properties = properties;
    }

    public List<AlertEvent> evaluate() {
        Instant now = Instant.now();
        List<AgentOpsAlert> currentAlerts = opsService.alerts();
        Set<String> currentFingerprints = currentAlerts.stream()
                .map(AlertEvent::fingerprint)
                .collect(Collectors.toSet());

        for (AgentOpsAlert alert : currentAlerts) {
            AlertEvent event = repository.findOpenByFingerprint(AlertEvent.fingerprint(alert))
                    .map(existing -> existing.observedAgain(alert, now, properties.getAlertNotificationCooldown()))
                    .orElseGet(() -> AlertEvent.open(repository.nextId(), alert, now));
            repository.save(event);
            notifyIfDue(event);
        }

        for (AlertEvent open : repository.findOpen()) {
            if (!currentFingerprints.contains(open.fingerprint())) {
                repository.save(open.resolved(now));
            }
        }

        return repository.findOpen();
    }

    public List<AlertEvent> list(AlertEventStatus status, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return repository.findAll().stream()
                .filter(event -> status == null || event.status() == status)
                .limit(safeLimit)
                .toList();
    }

    public AlertEvent acknowledge(Long id, Long userId) {
        AlertEvent event = get(id).acknowledged(userId, Instant.now());
        return repository.save(event);
    }

    public AlertEvent retryNotification(Long id) {
        AlertEvent event = get(id);
        notifyIfDue(repository.save(new AlertEvent(event.id(), event.fingerprint(), event.code(), event.severity(),
                event.status(), event.title(), event.message(), event.details(), event.occurrences(),
                event.firstTriggeredAt(), event.lastTriggeredAt(), event.acknowledgedAt(), event.acknowledgedBy(),
                event.resolvedAt(), AlertNotificationStatus.PENDING, event.notificationAttempts(),
                event.lastNotifiedAt(), Instant.now(), event.notificationError(), event.createdAt(),
                Instant.now())));
        return get(id);
    }

    public AlertEvent get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("告警事件不存在: " + id));
    }

    private void notifyIfDue(AlertEvent event) {
        Instant now = Instant.now();
        if (!properties.isAlertNotificationEnabled()) {
            repository.save(event.notificationDisabled(now));
            return;
        }
        if (!event.notificationDue(now, properties.getAlertNotificationMaxAttempts())) {
            return;
        }
        try {
            notifier.notify(event);
            repository.save(event.notificationSent(now, properties.getAlertNotificationCooldown()));
        } catch (RuntimeException exception) {
            repository.save(event.notificationFailed(exception.getMessage(), now,
                    properties.getAlertNotificationRetryDelay()));
        }
    }
}
