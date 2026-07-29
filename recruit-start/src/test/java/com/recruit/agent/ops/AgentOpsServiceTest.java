package com.recruit.agent.ops;

import com.recruit.agent.ai.domain.ModelCallLog;
import com.recruit.agent.ai.domain.ModelCallStatus;
import com.recruit.agent.ai.infrastructure.InMemoryModelCallLogRepository;
import com.recruit.agent.trace.AgentTraceEvent;
import com.recruit.agent.trace.AgentTraceStatus;
import com.recruit.agent.trace.InMemoryAgentTraceSink;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentOpsServiceTest {

    @Test
    void reportsDashboardAndAlertsForModelAndTraceFailures() {
        InMemoryModelCallLogRepository modelLogs = new InMemoryModelCallLogRepository();
        modelLogs.save(log(modelLogs.nextLogId(), ModelCallStatus.SUCCEEDED, 9_000L));
        modelLogs.save(log(modelLogs.nextLogId(), ModelCallStatus.FAILED, 10_000L));
        InMemoryAgentTraceSink traces = new InMemoryAgentTraceSink();
        traces.record(new AgentTraceEvent("trace-1", 1L, "tool.search_knowledge",
                AgentTraceStatus.FAILED, Map.of("component", "TOOL"), Instant.now()));
        traces.record(new AgentTraceEvent("trace-1", 1L, "model.reply",
                AgentTraceStatus.SUCCEEDED, Map.of("component", "MODEL"), Instant.now()));
        AgentOpsProperties properties = new AgentOpsProperties();
        properties.setModelFailureRateWarn(0.20);
        properties.setModelFailureRateCritical(0.40);
        properties.setModelLatencyWarnMillis(4_000);
        properties.setModelLatencyCriticalMillis(8_000);
        properties.setTraceFailureRateWarn(0.20);
        properties.setTraceFailureRateCritical(0.40);
        AgentOpsService service = new AgentOpsService(modelLogs, traces, properties);

        AgentOpsDashboard dashboard = service.dashboard();

        assertThat(dashboard.healthStatus()).isEqualTo(AgentOpsDashboard.HealthStatus.CRITICAL);
        assertThat(dashboard.model().failureRate()).isEqualTo(0.5);
        assertThat(dashboard.traces().failureRate()).isEqualTo(0.5);
        assertThat(dashboard.traceComponents()).containsEntry("TOOL", 1L).containsEntry("MODEL", 1L);
        assertThat(dashboard.activeAlerts()).extracting(AgentOpsAlert::code)
                .contains("MODEL_FAILURE_RATE", "MODEL_LATENCY", "TRACE_FAILURE_RATE");
    }

    @Test
    void reportsInfoAlertWhenNoModelCallsExist() {
        AgentOpsService service = new AgentOpsService(new InMemoryModelCallLogRepository(),
                new InMemoryAgentTraceSink(), new AgentOpsProperties());

        AgentOpsDashboard dashboard = service.dashboard();

        assertThat(dashboard.healthStatus()).isEqualTo(AgentOpsDashboard.HealthStatus.HEALTHY);
        assertThat(dashboard.activeAlerts()).extracting(AgentOpsAlert::code).contains("MODEL_NO_CALLS");
    }

    private ModelCallLog log(Long id, ModelCallStatus status, Long latencyMillis) {
        return new ModelCallLog(id, "REPLY_SUGGESTION", "deepseek", "v1",
                "system", "user", "{}", 10, 20, latencyMillis, status,
                status == ModelCallStatus.FAILED ? "failed" : null, Instant.now());
    }
}
