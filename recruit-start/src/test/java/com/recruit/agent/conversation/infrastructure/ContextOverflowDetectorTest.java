package com.recruit.agent.conversation.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextOverflowDetectorTest {
    private final ContextOverflowDetector detector = new ContextOverflowDetector();

    @Test
    void detectsProviderErrorThroughWrappedCauseChain() {
        RuntimeException failure = new IllegalStateException("LLM failed",
                new IllegalArgumentException("maximum context length exceeded"));

        assertThat(detector.isContextOverflow(failure)).isTrue();
        assertThat(detector.isContextOverflow(new IllegalStateException("HTTP 500"))).isFalse();
    }
}
