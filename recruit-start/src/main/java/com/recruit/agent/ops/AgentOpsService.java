package com.recruit.agent.ops;

import com.recruit.agent.ai.domain.ModelCallLog;
import com.recruit.agent.ai.domain.ModelCallLogRepository;
import com.recruit.agent.ai.domain.ModelCallStatus;
import com.recruit.agent.trace.AgentTraceEvent;
import com.recruit.agent.trace.AgentTraceSink;
import com.recruit.agent.trace.AgentTraceStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AgentOpsService {
    private final ModelCallLogRepository modelLogs;
    private final AgentTraceSink traceSink;
    private final AgentOpsProperties properties;

    public AgentOpsService(ModelCallLogRepository modelLogs, AgentTraceSink traceSink, AgentOpsProperties properties) {
        this.modelLogs = modelLogs;
        this.traceSink = traceSink;
        this.properties = properties;
    }

    public AgentOpsDashboard dashboard() {
        List<ModelCallLog> logs = recentModelLogs();
        List<AgentTraceEvent> traces = traceSink.findRecent(properties.getRecentLimit());
        AgentOpsDashboard.ModelOps model = modelOps(logs);
        AgentOpsDashboard.TraceOps traceOps = traceOps(traces);
        List<AgentOpsAlert> alerts = alerts(model, traceOps);
        return new AgentOpsDashboard(Instant.now(), healthStatus(alerts), model, traceOps,
                countByTraceComponent(traces), countByUseCase(logs), alerts);
    }

    public List<AgentOpsAlert> alerts() {
        AgentOpsDashboard dashboard = dashboard();
        return dashboard.activeAlerts();
    }

    private List<ModelCallLog> recentModelLogs() {
        int limit = Math.max(1, Math.min(properties.getRecentLimit(), 1000));
        return modelLogs.findAll().stream()
                .sorted(Comparator.comparing(ModelCallLog::createdAt).reversed())
                .limit(limit)
                .toList();
    }

    private AgentOpsDashboard.ModelOps modelOps(List<ModelCallLog> logs) {
        long succeeded = logs.stream().filter(log -> log.status() == ModelCallStatus.SUCCEEDED).count();
        long failed = logs.size() - succeeded;
        double averageLatency = logs.stream()
                .map(ModelCallLog::latencyMillis)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
        long promptTokens = logs.stream().map(ModelCallLog::promptTokens).filter(Objects::nonNull)
                .mapToLong(Integer::longValue).sum();
        long completionTokens = logs.stream().map(ModelCallLog::completionTokens).filter(Objects::nonNull)
                .mapToLong(Integer::longValue).sum();
        Instant latest = logs.stream().map(ModelCallLog::createdAt).max(Comparator.naturalOrder()).orElse(null);
        return new AgentOpsDashboard.ModelOps(logs.size(), succeeded, failed, rate(failed, logs.size()),
                averageLatency, promptTokens, completionTokens, latest);
    }

    private AgentOpsDashboard.TraceOps traceOps(List<AgentTraceEvent> traces) {
        long failed = traces.stream().filter(event -> event.status() == AgentTraceStatus.FAILED).count();
        Map<String, Long> components = countByTraceComponent(traces);
        return new AgentOpsDashboard.TraceOps(traces.size(), failed, rate(failed, traces.size()),
                components.getOrDefault("TOOL", 0L),
                components.getOrDefault("RETRIEVAL", 0L),
                components.getOrDefault("MODEL", 0L),
                components.getOrDefault("MEMORY", 0L),
                components.getOrDefault("SKILL", 0L));
    }

    private List<AgentOpsAlert> alerts(AgentOpsDashboard.ModelOps model, AgentOpsDashboard.TraceOps traces) {
        java.util.ArrayList<AgentOpsAlert> alerts = new java.util.ArrayList<>();
        if (model.totalCalls() == 0) {
            alerts.add(new AgentOpsAlert("MODEL_NO_CALLS", AlertSeverity.INFO, "No model calls yet",
                    "No model call log has been recorded in the recent window.", Map.of(), Instant.now()));
        } else {
            addRateAlert(alerts, "MODEL_FAILURE_RATE", "Model failure rate is high", model.failureRate(),
                    properties.getModelFailureRateWarn(), properties.getModelFailureRateCritical());
            addLatencyAlert(alerts, model.averageLatencyMillis());
            if (model.latestCallAt() != null
                    && model.latestCallAt().isBefore(Instant.now().minus(properties.getNoModelCallWarnAfter()))) {
                alerts.add(new AgentOpsAlert("MODEL_STALE_CALLS", AlertSeverity.WARN, "Model calls are stale",
                        "No recent model call has been recorded.", Map.of("latestCallAt", model.latestCallAt()),
                        Instant.now()));
            }
        }
        if (traces.totalEvents() > 0) {
            addRateAlert(alerts, "TRACE_FAILURE_RATE", "Agent trace failure rate is high", traces.failureRate(),
                    properties.getTraceFailureRateWarn(), properties.getTraceFailureRateCritical());
        }
        return List.copyOf(alerts);
    }

    private void addRateAlert(List<AgentOpsAlert> alerts, String code, String title, double value,
                              double warn, double critical) {
        if (value >= critical) {
            alerts.add(new AgentOpsAlert(code, AlertSeverity.CRITICAL, title,
                    "Failure rate reached " + percent(value) + ".", Map.of("rate", value), Instant.now()));
        } else if (value >= warn) {
            alerts.add(new AgentOpsAlert(code, AlertSeverity.WARN, title,
                    "Failure rate reached " + percent(value) + ".", Map.of("rate", value), Instant.now()));
        }
    }

    private void addLatencyAlert(List<AgentOpsAlert> alerts, double latency) {
        if (latency >= properties.getModelLatencyCriticalMillis()) {
            alerts.add(new AgentOpsAlert("MODEL_LATENCY", AlertSeverity.CRITICAL, "Model latency is critical",
                    "Average model latency reached " + Math.round(latency) + " ms.",
                    Map.of("averageLatencyMillis", latency), Instant.now()));
        } else if (latency >= properties.getModelLatencyWarnMillis()) {
            alerts.add(new AgentOpsAlert("MODEL_LATENCY", AlertSeverity.WARN, "Model latency is high",
                    "Average model latency reached " + Math.round(latency) + " ms.",
                    Map.of("averageLatencyMillis", latency), Instant.now()));
        }
    }

    private AgentOpsDashboard.HealthStatus healthStatus(List<AgentOpsAlert> alerts) {
        if (alerts.stream().anyMatch(alert -> alert.severity() == AlertSeverity.CRITICAL)) {
            return AgentOpsDashboard.HealthStatus.CRITICAL;
        }
        if (alerts.stream().anyMatch(alert -> alert.severity() == AlertSeverity.WARN)) {
            return AgentOpsDashboard.HealthStatus.DEGRADED;
        }
        return AgentOpsDashboard.HealthStatus.HEALTHY;
    }

    private Map<String, Long> countByTraceComponent(List<AgentTraceEvent> traces) {
        Map<String, Long> grouped = traces.stream()
                .collect(Collectors.groupingBy(this::component, Collectors.counting()));
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private Map<String, Long> countByUseCase(List<ModelCallLog> logs) {
        Map<String, Long> grouped = logs.stream()
                .collect(Collectors.groupingBy(ModelCallLog::useCase, Collectors.counting()));
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private String component(AgentTraceEvent event) {
        Object component = event.details().get("component");
        return component == null ? "UNKNOWN" : String.valueOf(component);
    }

    private double rate(long numerator, long denominator) {
        return denominator <= 0 ? 0 : (double) numerator / denominator;
    }

    private String percent(double value) {
        return Math.round(value * 1000.0) / 10.0 + "%";
    }
}
