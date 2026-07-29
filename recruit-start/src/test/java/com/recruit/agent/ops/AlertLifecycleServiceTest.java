package com.recruit.agent.ops;

import com.recruit.agent.ai.domain.ModelCallLog;
import com.recruit.agent.ai.domain.ModelCallStatus;
import com.recruit.agent.ai.infrastructure.InMemoryModelCallLogRepository;
import com.recruit.agent.trace.InMemoryAgentTraceSink;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AlertLifecycleServiceTest {

    @Test
    void persistsDeduplicatesAcknowledgesAndResolvesAlerts() {
        InMemoryModelCallLogRepository modelLogs = new InMemoryModelCallLogRepository();
        modelLogs.save(log(modelLogs.nextLogId(), ModelCallStatus.FAILED));
        RecordingNotifier notifier = new RecordingNotifier();
        InMemoryAlertEventRepository repository = new InMemoryAlertEventRepository();
        AlertLifecycleService service = service(modelLogs, repository, notifier);

        List<AlertEvent> first = service.evaluate();
        List<AlertEvent> second = service.evaluate();

        assertThat(first).hasSize(2);
        assertThat(second).hasSize(2);
        assertThat(repository.findAll()).hasSize(2);
        assertThat(notifier.events).hasSize(2);

        AlertEvent event = repository.findAll().stream()
                .filter(alert -> alert.code().equals("MODEL_FAILURE_RATE"))
                .findFirst()
                .orElseThrow();
        AlertEvent acknowledged = service.acknowledge(event.id(), 7L);
        assertThat(acknowledged.status()).isEqualTo(AlertEventStatus.ACKNOWLEDGED);
        assertThat(acknowledged.acknowledgedBy()).isEqualTo(7L);

        InMemoryModelCallLogRepository healthyLogs = new InMemoryModelCallLogRepository();
        healthyLogs.save(log(healthyLogs.nextLogId(), ModelCallStatus.SUCCEEDED, 100L));
        AlertLifecycleService healthyService = service(healthyLogs, repository, notifier);
        healthyService.evaluate();

        assertThat(repository.findAll()).filteredOn(alert -> alert.status() == AlertEventStatus.RESOLVED)
                .hasSize(2);
    }

    @Test
    void recordsFailedNotificationAndRetriesManually() {
        InMemoryModelCallLogRepository modelLogs = new InMemoryModelCallLogRepository();
        modelLogs.save(log(modelLogs.nextLogId(), ModelCallStatus.FAILED));
        RecordingNotifier notifier = new RecordingNotifier();
        notifier.fail = true;
        InMemoryAlertEventRepository repository = new InMemoryAlertEventRepository();
        AlertLifecycleService service = service(modelLogs, repository, notifier);

        service.evaluate();

        AlertEvent failed = repository.findAll().stream()
                .filter(alert -> alert.notificationStatus() == AlertNotificationStatus.FAILED)
                .findFirst()
                .orElseThrow();
        assertThat(failed.notificationAttempts()).isEqualTo(1);

        notifier.fail = false;
        AlertEvent retried = service.retryNotification(failed.id());

        assertThat(retried.notificationStatus()).isEqualTo(AlertNotificationStatus.SENT);
        assertThat(retried.notificationAttempts()).isEqualTo(2);
    }

    private AlertLifecycleService service(InMemoryModelCallLogRepository modelLogs,
                                          InMemoryAlertEventRepository repository,
                                          RecordingNotifier notifier) {
        AgentOpsProperties properties = new AgentOpsProperties();
        properties.setModelFailureRateWarn(0.20);
        properties.setModelFailureRateCritical(0.40);
        properties.setModelLatencyWarnMillis(4_000);
        properties.setModelLatencyCriticalMillis(8_000);
        properties.setAlertNotificationEnabled(true);
        properties.setAlertNotificationCooldown(Duration.ofMinutes(10));
        properties.setAlertNotificationRetryDelay(Duration.ZERO);
        AgentOpsService opsService = new AgentOpsService(modelLogs, new InMemoryAgentTraceSink(), properties);
        return new AlertLifecycleService(opsService, repository, notifier, properties);
    }

    private ModelCallLog log(Long id, ModelCallStatus status) {
        return log(id, status, 9_000L);
    }

    private ModelCallLog log(Long id, ModelCallStatus status, Long latencyMillis) {
        return new ModelCallLog(id, "REPLY_SUGGESTION", "deepseek", "v1",
                "system", "user", "{}", 10, 20, latencyMillis, status,
                status == ModelCallStatus.FAILED ? "failed" : null, Instant.now());
    }

    private static class RecordingNotifier implements AlertNotifier {
        private final List<AlertEvent> events = new ArrayList<>();
        private boolean fail;

        @Override
        public void notify(AlertEvent event) {
            if (fail) {
                throw new IllegalStateException("webhook unavailable");
            }
            events.add(event);
        }
    }
}
