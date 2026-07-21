package com.recruit.agent.trace.api;

import com.recruit.agent.trace.AgentTraceEvent;
import com.recruit.agent.trace.AgentTraceSink;
import com.recruit.agent.trace.AgentTraceStatus;
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
}
