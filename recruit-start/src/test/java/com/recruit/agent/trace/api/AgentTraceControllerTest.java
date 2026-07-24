package com.recruit.agent.trace.api;

import com.recruit.agent.trace.AgentTraceEvent;
import com.recruit.agent.trace.AgentTraceSink;
import com.recruit.agent.trace.AgentTraceStatus;
import com.recruit.agent.trace.TraceComponent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentTraceControllerTest {
    private final AgentTraceSink sink = mock(AgentTraceSink.class);
    private final AgentTraceController controller = new AgentTraceController(sink);

    @Test
    void findsTraceByConversation() {
        AgentTraceEvent event = new AgentTraceEvent("trace-1", 7L, "route", AgentTraceStatus.SUCCEEDED,
                Map.of("route", "JOB_INFO"), Instant.EPOCH);
        when(sink.findByConversationId(7L)).thenReturn(List.of(event));

        assertThat(controller.find(7L, null)).containsExactly(AgentTraceResponse.from(event));
        verify(sink).findByConversationId(7L);
    }

    @Test
    void requiresAQueryKey() {
        assertThatThrownBy(() -> controller.find(null, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conversationId or traceId");
    }

    @Test
    void filtersUnifiedEventsByComponentAndStatus() {
        AgentTraceEvent agent = new AgentTraceEvent("trace-1", 7L, "route", AgentTraceStatus.SUCCEEDED,
                Map.of("component", "AGENT"), Instant.EPOCH);
        AgentTraceEvent model = new AgentTraceEvent("trace-1", 7L, "model.reply", AgentTraceStatus.FAILED,
                Map.of("component", "MODEL", "operation", "reply"), Instant.EPOCH.plusSeconds(1));
        when(sink.findByTraceId("trace-1")).thenReturn(List.of(agent, model));

        assertThat(controller.find(null, "trace-1", TraceComponent.MODEL, AgentTraceStatus.FAILED))
                .containsExactly(AgentTraceResponse.from(model));
    }
}
