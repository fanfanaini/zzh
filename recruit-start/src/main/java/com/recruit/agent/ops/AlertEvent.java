package com.recruit.agent.ops;

import java.time.Instant;
import java.util.Map;

public record AlertEvent(
        Long id,
        String fingerprint,
        String code,
        AlertSeverity severity,
        AlertEventStatus status,
        String title,
        String message,
        Map<String, Object> details,
        int occurrences,
        Instant firstTriggeredAt,
        Instant lastTriggeredAt,
        Instant acknowledgedAt,
        Long acknowledgedBy,
        Instant resolvedAt,
        AlertNotificationStatus notificationStatus,
        int notificationAttempts,
        Instant lastNotifiedAt,
        Instant nextNotifyAt,
        String notificationError,
        Instant createdAt,
        Instant updatedAt
) {

    public AlertEvent {
        if (id == null) {
            throw new IllegalArgumentException("告警事件 ID 不能为空");
        }
        if (fingerprint == null || fingerprint.isBlank()) {
            throw new IllegalArgumentException("告警指纹不能为空");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("告警编码不能为空");
        }
        severity = severity == null ? AlertSeverity.INFO : severity;
        status = status == null ? AlertEventStatus.ACTIVE : status;
        title = title == null ? "" : title.trim();
        message = message == null ? "" : message.trim();
        details = details == null ? Map.of() : Map.copyOf(details);
        occurrences = Math.max(occurrences, 1);
        if (firstTriggeredAt == null) {
            firstTriggeredAt = Instant.now();
        }
        if (lastTriggeredAt == null) {
            lastTriggeredAt = firstTriggeredAt;
        }
        notificationStatus = notificationStatus == null ? AlertNotificationStatus.PENDING : notificationStatus;
        if (createdAt == null) {
            createdAt = firstTriggeredAt;
        }
        if (updatedAt == null) {
            updatedAt = lastTriggeredAt;
        }
    }

    public static AlertEvent open(Long id, AgentOpsAlert alert, Instant now) {
        return new AlertEvent(id, fingerprint(alert), alert.code(), alert.severity(), AlertEventStatus.ACTIVE,
                alert.title(), alert.message(), alert.details(), 1, now, now, null, null, null,
                AlertNotificationStatus.PENDING, 0, null, now, null, now, now);
    }

    public AlertEvent observedAgain(AgentOpsAlert alert, Instant now, java.time.Duration cooldown) {
        boolean severityChanged = alert.severity().ordinal() > severity.ordinal();
        AlertNotificationStatus nextStatus = notificationStatus;
        Instant nextNotify = nextNotifyAt;
        if (severityChanged || nextNotifyAt == null || !nextNotifyAt.isAfter(now)) {
            nextStatus = AlertNotificationStatus.PENDING;
            nextNotify = now.plus(cooldown);
        }
        return new AlertEvent(id, fingerprint, code, alert.severity(), status == AlertEventStatus.RESOLVED
                ? AlertEventStatus.ACTIVE : status, alert.title(), alert.message(), alert.details(),
                occurrences + 1, firstTriggeredAt, now, acknowledgedAt, acknowledgedBy, null, nextStatus,
                notificationAttempts, lastNotifiedAt, nextNotify, notificationError, createdAt, now);
    }

    public AlertEvent acknowledged(Long userId, Instant now) {
        if (status == AlertEventStatus.RESOLVED) {
            return this;
        }
        return new AlertEvent(id, fingerprint, code, severity, AlertEventStatus.ACKNOWLEDGED, title, message,
                details, occurrences, firstTriggeredAt, lastTriggeredAt, now, userId, resolvedAt,
                notificationStatus, notificationAttempts, lastNotifiedAt, nextNotifyAt, notificationError,
                createdAt, now);
    }

    public AlertEvent resolved(Instant now) {
        return new AlertEvent(id, fingerprint, code, severity, AlertEventStatus.RESOLVED, title, message,
                details, occurrences, firstTriggeredAt, lastTriggeredAt, acknowledgedAt, acknowledgedBy, now,
                notificationStatus, notificationAttempts, lastNotifiedAt, nextNotifyAt, notificationError,
                createdAt, now);
    }

    public AlertEvent notificationSent(Instant now, java.time.Duration cooldown) {
        return new AlertEvent(id, fingerprint, code, severity, status, title, message, details, occurrences,
                firstTriggeredAt, lastTriggeredAt, acknowledgedAt, acknowledgedBy, resolvedAt,
                AlertNotificationStatus.SENT, notificationAttempts + 1, now, now.plus(cooldown), null,
                createdAt, now);
    }

    public AlertEvent notificationFailed(String error, Instant now, java.time.Duration retryDelay) {
        return new AlertEvent(id, fingerprint, code, severity, status, title, message, details, occurrences,
                firstTriggeredAt, lastTriggeredAt, acknowledgedAt, acknowledgedBy, resolvedAt,
                AlertNotificationStatus.FAILED, notificationAttempts + 1, lastNotifiedAt, now.plus(retryDelay),
                error == null ? "notification failed" : error, createdAt, now);
    }

    public AlertEvent notificationDisabled(Instant now) {
        return new AlertEvent(id, fingerprint, code, severity, status, title, message, details, occurrences,
                firstTriggeredAt, lastTriggeredAt, acknowledgedAt, acknowledgedBy, resolvedAt,
                AlertNotificationStatus.DISABLED, notificationAttempts, lastNotifiedAt, nextNotifyAt,
                notificationError, createdAt, now);
    }

    public boolean notificationDue(Instant now, int maxAttempts) {
        return status != AlertEventStatus.RESOLVED
                && notificationStatus != AlertNotificationStatus.DISABLED
                && notificationAttempts < maxAttempts
                && (notificationStatus == AlertNotificationStatus.PENDING
                || notificationStatus == AlertNotificationStatus.FAILED)
                && (nextNotifyAt == null || !nextNotifyAt.isAfter(now));
    }

    public static String fingerprint(AgentOpsAlert alert) {
        return alert.code() + ":" + alert.severity().name();
    }
}
