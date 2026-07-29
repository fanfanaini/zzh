package com.recruit.agent.ops;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.MeterRegistry;

public class AgentOpsMetrics implements MeterBinder {
    private final AgentOpsService service;

    public AgentOpsMetrics(AgentOpsService service) {
        this.service = service;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("recruit_agent_model_failure_rate", service,
                        ops -> ops.dashboard().model().failureRate())
                .description("Recent model call failure rate")
                .register(registry);
        Gauge.builder("recruit_agent_model_latency_avg_millis", service,
                        ops -> ops.dashboard().model().averageLatencyMillis())
                .description("Recent average model call latency in milliseconds")
                .register(registry);
        Gauge.builder("recruit_agent_trace_failure_rate", service,
                        ops -> ops.dashboard().traces().failureRate())
                .description("Recent agent trace failure rate")
                .register(registry);
        Gauge.builder("recruit_agent_active_alerts", service,
                        ops -> ops.alerts().size())
                .description("Current active ops alerts")
                .register(registry);
    }
}
