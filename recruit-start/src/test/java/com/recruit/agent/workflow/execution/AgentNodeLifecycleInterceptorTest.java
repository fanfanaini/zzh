package com.recruit.agent.workflow.execution;

import com.recruit.agent.trace.AgentTraceStatus;
import com.recruit.agent.trace.InMemoryAgentTraceSink;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentNodeLifecycleInterceptorTest {
    private final InMemoryAgentTraceSink traces = new InMemoryAgentTraceSink();
    private final AgentNodeLifecycleInterceptor interceptor = new AgentNodeLifecycleInterceptor(traces);

    @Test
    void recordsStartedAndSucceededWithExecutionIdentityAndDuration() {
        var updates = interceptor.execute(context(), "route", () ->
                new AgentNodeResult(Map.of("route", "JOB_INFO"), Map.of("selectedRoute", "JOB_INFO")));

        assertThat(updates).containsEntry("route", "JOB_INFO");
        assertThat(traces.findByTraceId("trace-1")).extracting("status")
                .containsExactly(AgentTraceStatus.STARTED, AgentTraceStatus.SUCCEEDED);
        assertThat(traces.findByTraceId("trace-1").get(1).details())
                .containsEntry("principalId", "42")
                .containsEntry("attempt", 1)
                .containsEntry("selectedRoute", "JOB_INFO")
                .containsKey("durationMillis");
    }

    @Test
    void recordsTypedFailureAndRethrows() {
        assertThatThrownBy(() -> interceptor.execute(context(), "tool", () -> {
            throw new IllegalStateException("tool unavailable");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(traces.findByTraceId("trace-1")).extracting("status")
                .containsExactly(AgentTraceStatus.STARTED, AgentTraceStatus.FAILED);
        assertThat(traces.findByTraceId("trace-1").get(1).details())
                .containsEntry("errorCode", "IllegalStateException")
                .containsEntry("error", "tool unavailable");
    }

    private AgentExecutionContext context() {
        return new AgentExecutionContext("trace-1", 3L, "tenant-a", null, "42", Set.of("HR"),
                Set.of(AgentPermissions.RECRUIT_READ), "reply/v2", "deepseek-chat", Map.of());
    }
}
