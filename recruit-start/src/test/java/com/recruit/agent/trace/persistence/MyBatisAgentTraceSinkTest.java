package com.recruit.agent.trace.persistence;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.trace.AgentTraceEvent;
import com.recruit.agent.trace.AgentTraceStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MyBatisAgentTraceSinkTest {
    private final AgentTraceMapper mapper = mock(AgentTraceMapper.class);
    private final MyBatisAgentTraceSink sink = new MyBatisAgentTraceSink(mapper, new ObjectMapper());

    @Test
    void persistsStructuredTraceDetails() {
        AgentTraceEvent event = new AgentTraceEvent("trace-1", 9L, "route", AgentTraceStatus.SUCCEEDED,
                Map.of("route", "JOB_INFO", "confidence", 0.88), Instant.parse("2026-07-21T06:00:00Z"));

        sink.record(event);

        ArgumentCaptor<AgentTracePo> captor = ArgumentCaptor.forClass(AgentTracePo.class);
        verify(mapper).insert(captor.capture());
        AgentTracePo saved = captor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTraceId()).isEqualTo("trace-1");
        assertThat(saved.getConversationId()).isEqualTo(9L);
        assertThat(saved.getDetailsJson()).contains("JOB_INFO").contains("confidence");
        assertThat(saved.getOccurredAt()).isEqualTo(LocalDateTime.parse("2026-07-21T06:00:00"));
    }

    @Test
    void restoresEventsFromJson() {
        AgentTracePo po = new AgentTracePo();
        po.setId(1L);
        po.setTraceId("trace-2");
        po.setConversationId(10L);
        po.setNode("tool_context");
        po.setStatus("FAILED");
        po.setDetailsJson("{\"errorCode\":\"TOOL_TIMEOUT\"}");
        po.setOccurredAt(LocalDateTime.parse("2026-07-21T06:01:00"));
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(po));

        assertThat(sink.findByTraceId("trace-2")).singleElement().satisfies(event -> {
            assertThat(event.status()).isEqualTo(AgentTraceStatus.FAILED);
            assertThat(event.details()).containsEntry("errorCode", "TOOL_TIMEOUT");
            assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-07-21T06:01:00Z"));
        });
    }
}
