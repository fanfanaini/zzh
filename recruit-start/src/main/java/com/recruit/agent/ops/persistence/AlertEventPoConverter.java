package com.recruit.agent.ops.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.ops.AlertEvent;
import com.recruit.agent.ops.AlertEventStatus;
import com.recruit.agent.ops.AlertNotificationStatus;
import com.recruit.agent.ops.AlertSeverity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public class AlertEventPoConverter {

    private final ObjectMapper objectMapper;

    public AlertEventPoConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AlertEventPo toPo(AlertEvent event) {
        AlertEventPo po = new AlertEventPo();
        po.setId(event.id());
        po.setFingerprint(event.fingerprint());
        po.setCode(event.code());
        po.setSeverity(event.severity().name());
        po.setStatus(event.status().name());
        po.setTitle(event.title());
        po.setMessage(event.message());
        po.setDetailsJson(writeDetails(event.details()));
        po.setOccurrences(event.occurrences());
        po.setFirstTriggeredAt(time(event.firstTriggeredAt()));
        po.setLastTriggeredAt(time(event.lastTriggeredAt()));
        po.setAcknowledgedAt(time(event.acknowledgedAt()));
        po.setAcknowledgedBy(event.acknowledgedBy());
        po.setResolvedAt(time(event.resolvedAt()));
        po.setNotificationStatus(event.notificationStatus().name());
        po.setNotificationAttempts(event.notificationAttempts());
        po.setLastNotifiedAt(time(event.lastNotifiedAt()));
        po.setNextNotifyAt(time(event.nextNotifyAt()));
        po.setNotificationError(event.notificationError());
        po.setCreatedAt(time(event.createdAt()));
        po.setUpdatedAt(time(event.updatedAt()));
        return po;
    }

    public AlertEvent toDomain(AlertEventPo po) {
        return new AlertEvent(po.getId(), po.getFingerprint(), po.getCode(),
                AlertSeverity.valueOf(po.getSeverity()),
                AlertEventStatus.valueOf(po.getStatus()),
                po.getTitle(), po.getMessage(), readDetails(po.getDetailsJson()),
                po.getOccurrences() == null ? 1 : po.getOccurrences(),
                instant(po.getFirstTriggeredAt()),
                instant(po.getLastTriggeredAt()),
                instant(po.getAcknowledgedAt()),
                po.getAcknowledgedBy(),
                instant(po.getResolvedAt()),
                po.getNotificationStatus() == null
                        ? AlertNotificationStatus.PENDING
                        : AlertNotificationStatus.valueOf(po.getNotificationStatus()),
                po.getNotificationAttempts() == null ? 0 : po.getNotificationAttempts(),
                instant(po.getLastNotifiedAt()),
                instant(po.getNextNotifyAt()),
                po.getNotificationError(),
                instant(po.getCreatedAt()),
                instant(po.getUpdatedAt()));
    }

    private String writeDetails(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details == null ? Map.of() : details);
        } catch (Exception exception) {
            throw new IllegalArgumentException("告警详情序列化失败", exception);
        }
    }

    private Map<String, Object> readDetails(String detailsJson) {
        if (detailsJson == null || detailsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(detailsJson, new TypeReference<>() {});
        } catch (Exception exception) {
            return Map.of("raw", detailsJson);
        }
    }

    private LocalDateTime time(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant instant(LocalDateTime time) {
        return time == null ? null : time.toInstant(ZoneOffset.UTC);
    }
}
