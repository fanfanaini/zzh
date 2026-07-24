package com.recruit.agent.trace;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentTraceEventPrivacyTest {

    @Test
    void recursivelyRedactsDetailsBeforeTheyReachAnySink() {
        AgentTraceEvent event = new AgentTraceEvent(
                "trace-1",
                1L,
                "model.reply",
                AgentTraceStatus.FAILED,
                Map.of("payload", List.of(
                        "email candidate@example.com",
                        Map.of("authorization", "Bearer secret-token"))),
                Instant.EPOCH
        );

        assertThat(event.details().toString())
                .contains("[REDACTED_EMAIL]", "[REDACTED_BEARER_TOKEN]")
                .doesNotContain("candidate@example.com", "secret-token");
    }
}
