package com.recruit.agent.trace;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedTraceRecorderTest {

    @Test
    void recordsCorrelatedStartedAndSucceededEvents() {
        InMemoryAgentTraceSink sink = new InMemoryAgentTraceSink();
        UnifiedTraceRecorder recorder = new UnifiedTraceRecorder(sink);

        try (TraceContextHolder.Scope ignored = TraceContextHolder.open(
                new TraceContext("trace-1", 7L, "node-span"))) {
            UnifiedTraceRecorder.Span span = recorder.start(
                    TraceComponent.RETRIEVAL, "hybrid_search", Map.of("queryLength", 12));
            span.succeed(Map.of("resultCount", 3));
        }

        assertThat(sink.findByTraceId("trace-1")).hasSize(2).allSatisfy(event -> {
            assertThat(event.conversationId()).isEqualTo(7L);
            assertThat(event.details()).containsEntry("component", "RETRIEVAL")
                    .containsEntry("operation", "hybrid_search")
                    .containsEntry("parentSpanId", "node-span");
        });
        assertThat(sink.findByTraceId("trace-1"))
                .extracting(AgentTraceEvent::status)
                .containsExactly(AgentTraceStatus.STARTED, AgentTraceStatus.SUCCEEDED);
    }

    @Test
    void doesNotCreateOrphanEventsWithoutExecutionContext() {
        InMemoryAgentTraceSink sink = new InMemoryAgentTraceSink();
        UnifiedTraceRecorder recorder = new UnifiedTraceRecorder(sink);

        recorder.start(TraceComponent.MODEL, "reply", Map.of()).succeed(Map.of());

        assertThat(sink.findByTraceId("missing")).isEmpty();
    }
}
