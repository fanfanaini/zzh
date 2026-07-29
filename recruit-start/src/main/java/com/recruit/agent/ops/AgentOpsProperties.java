package com.recruit.agent.ops;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "recruit.ops")
public class AgentOpsProperties {
    private int recentLimit = 200;
    private double modelFailureRateWarn = 0.20;
    private double modelFailureRateCritical = 0.40;
    private long modelLatencyWarnMillis = 4_000;
    private long modelLatencyCriticalMillis = 8_000;
    private double traceFailureRateWarn = 0.15;
    private double traceFailureRateCritical = 0.30;
    private Duration noModelCallWarnAfter = Duration.ofHours(24);
    private boolean alertSchedulerEnabled = true;
    private long alertEvaluationIntervalMs = 60_000;
    private boolean alertNotificationEnabled = false;
    private String alertWebhookUrl = "";
    private Duration alertNotificationCooldown = Duration.ofMinutes(10);
    private Duration alertNotificationRetryDelay = Duration.ofMinutes(1);
    private int alertNotificationMaxAttempts = 5;

    public int getRecentLimit() {
        return recentLimit;
    }

    public void setRecentLimit(int recentLimit) {
        this.recentLimit = recentLimit;
    }

    public double getModelFailureRateWarn() {
        return modelFailureRateWarn;
    }

    public void setModelFailureRateWarn(double modelFailureRateWarn) {
        this.modelFailureRateWarn = modelFailureRateWarn;
    }

    public double getModelFailureRateCritical() {
        return modelFailureRateCritical;
    }

    public void setModelFailureRateCritical(double modelFailureRateCritical) {
        this.modelFailureRateCritical = modelFailureRateCritical;
    }

    public long getModelLatencyWarnMillis() {
        return modelLatencyWarnMillis;
    }

    public void setModelLatencyWarnMillis(long modelLatencyWarnMillis) {
        this.modelLatencyWarnMillis = modelLatencyWarnMillis;
    }

    public long getModelLatencyCriticalMillis() {
        return modelLatencyCriticalMillis;
    }

    public void setModelLatencyCriticalMillis(long modelLatencyCriticalMillis) {
        this.modelLatencyCriticalMillis = modelLatencyCriticalMillis;
    }

    public double getTraceFailureRateWarn() {
        return traceFailureRateWarn;
    }

    public void setTraceFailureRateWarn(double traceFailureRateWarn) {
        this.traceFailureRateWarn = traceFailureRateWarn;
    }

    public double getTraceFailureRateCritical() {
        return traceFailureRateCritical;
    }

    public void setTraceFailureRateCritical(double traceFailureRateCritical) {
        this.traceFailureRateCritical = traceFailureRateCritical;
    }

    public Duration getNoModelCallWarnAfter() {
        return noModelCallWarnAfter;
    }

    public void setNoModelCallWarnAfter(Duration noModelCallWarnAfter) {
        this.noModelCallWarnAfter = noModelCallWarnAfter;
    }

    public boolean isAlertSchedulerEnabled() {
        return alertSchedulerEnabled;
    }

    public void setAlertSchedulerEnabled(boolean alertSchedulerEnabled) {
        this.alertSchedulerEnabled = alertSchedulerEnabled;
    }

    public long getAlertEvaluationIntervalMs() {
        return alertEvaluationIntervalMs;
    }

    public void setAlertEvaluationIntervalMs(long alertEvaluationIntervalMs) {
        this.alertEvaluationIntervalMs = alertEvaluationIntervalMs;
    }

    public boolean isAlertNotificationEnabled() {
        return alertNotificationEnabled;
    }

    public void setAlertNotificationEnabled(boolean alertNotificationEnabled) {
        this.alertNotificationEnabled = alertNotificationEnabled;
    }

    public String getAlertWebhookUrl() {
        return alertWebhookUrl;
    }

    public void setAlertWebhookUrl(String alertWebhookUrl) {
        this.alertWebhookUrl = alertWebhookUrl;
    }

    public Duration getAlertNotificationCooldown() {
        return alertNotificationCooldown;
    }

    public void setAlertNotificationCooldown(Duration alertNotificationCooldown) {
        this.alertNotificationCooldown = alertNotificationCooldown;
    }

    public Duration getAlertNotificationRetryDelay() {
        return alertNotificationRetryDelay;
    }

    public void setAlertNotificationRetryDelay(Duration alertNotificationRetryDelay) {
        this.alertNotificationRetryDelay = alertNotificationRetryDelay;
    }

    public int getAlertNotificationMaxAttempts() {
        return alertNotificationMaxAttempts;
    }

    public void setAlertNotificationMaxAttempts(int alertNotificationMaxAttempts) {
        this.alertNotificationMaxAttempts = alertNotificationMaxAttempts;
    }
}
