package com.recruit.agent.ops.persistence;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("alert_event")
public class AlertEventPo {
    @TableId
    private Long id;
    private String fingerprint;
    private String code;
    private String severity;
    private String status;
    private String title;
    private String message;
    private String detailsJson;
    private Integer occurrences;
    private LocalDateTime firstTriggeredAt;
    private LocalDateTime lastTriggeredAt;
    private LocalDateTime acknowledgedAt;
    private Long acknowledgedBy;
    private LocalDateTime resolvedAt;
    private String notificationStatus;
    private Integer notificationAttempts;
    private LocalDateTime lastNotifiedAt;
    private LocalDateTime nextNotifyAt;
    private String notificationError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
    public Integer getOccurrences() { return occurrences; }
    public void setOccurrences(Integer occurrences) { this.occurrences = occurrences; }
    public LocalDateTime getFirstTriggeredAt() { return firstTriggeredAt; }
    public void setFirstTriggeredAt(LocalDateTime firstTriggeredAt) { this.firstTriggeredAt = firstTriggeredAt; }
    public LocalDateTime getLastTriggeredAt() { return lastTriggeredAt; }
    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public Long getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(Long acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getNotificationStatus() { return notificationStatus; }
    public void setNotificationStatus(String notificationStatus) { this.notificationStatus = notificationStatus; }
    public Integer getNotificationAttempts() { return notificationAttempts; }
    public void setNotificationAttempts(Integer notificationAttempts) { this.notificationAttempts = notificationAttempts; }
    public LocalDateTime getLastNotifiedAt() { return lastNotifiedAt; }
    public void setLastNotifiedAt(LocalDateTime lastNotifiedAt) { this.lastNotifiedAt = lastNotifiedAt; }
    public LocalDateTime getNextNotifyAt() { return nextNotifyAt; }
    public void setNextNotifyAt(LocalDateTime nextNotifyAt) { this.nextNotifyAt = nextNotifyAt; }
    public String getNotificationError() { return notificationError; }
    public void setNotificationError(String notificationError) { this.notificationError = notificationError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
