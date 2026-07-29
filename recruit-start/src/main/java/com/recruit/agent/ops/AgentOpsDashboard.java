package com.recruit.agent.ops;

import java.time.Instant;
import java.util.Map;

public record AgentOpsDashboard(
        Instant generatedAt,
        HealthStatus healthStatus,
        ModelOps model,
        TraceOps traces,
        Map<String, Long> traceComponents,
        Map<String, Long> modelUseCases,
        java.util.List<AgentOpsAlert> activeAlerts
) {
    public enum HealthStatus {
        HEALTHY,
        DEGRADED,
        CRITICAL
    }

    public record ModelOps(
            long totalCalls,
            long succeededCalls,
            long failedCalls,
            double failureRate,
            double averageLatencyMillis,
            long promptTokens,
            long completionTokens,
            Instant latestCallAt
    ) {
    }

    public record TraceOps(
            long totalEvents,
            long failedEvents,
            double failureRate,
            long toolEvents,
            long retrievalEvents,
            long modelEvents,
            long memoryEvents,
            long skillEvents
    ) {
    }
}
