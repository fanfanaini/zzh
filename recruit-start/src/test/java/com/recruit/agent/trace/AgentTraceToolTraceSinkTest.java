package com.recruit.agent.trace;

import com.recruit.agent.tool.ToolError;
import com.recruit.agent.tool.ToolErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTraceToolTraceSinkTest {

    @Test
    void persistsToolTraceUnderTheRootExecutionTrace() {
        InMemoryAgentTraceSink sink = new InMemoryAgentTraceSink();
        AgentTraceToolTraceSink toolSink = new AgentTraceToolTraceSink(new UnifiedTraceRecorder(sink));
        ToolTrace trace = new ToolTrace(
                "tool-span",
                "root-trace",
                "search_knowledge",
                "hr-7",
                9L,
                Map.of("query", "candidate@example.com"),
                ToolTraceStatus.FAILED,
                2,
                new ToolError(ToolErrorCode.TIMEOUT, "contact 13812345678", Map.of()),
                Instant.EPOCH,
                Instant.EPOCH.plusMillis(25)
        );

        toolSink.record(trace);

        assertThat(sink.findByTraceId("root-trace")).singleElement().satisfies(event -> {
            assertThat(event.conversationId()).isEqualTo(9L);
            assertThat(event.node()).isEqualTo("tool.search_knowledge");
            assertThat(event.status()).isEqualTo(AgentTraceStatus.FAILED);
            assertThat(event.details()).containsEntry("spanId", "tool-span")
                    .containsEntry("durationMillis", 25L)
                    .containsEntry("error", "contact [REDACTED_PHONE]");
            assertThat(event.details().toString()).doesNotContain("candidate@example.com", "13812345678");
        });
    }
}
