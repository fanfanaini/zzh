package com.recruit.agent.conversation.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicTokenEstimatorTest {
    private final HeuristicTokenEstimator estimator = new HeuristicTokenEstimator();

    @Test
    void estimatesCjkAndAsciiWithoutSplittingUnicodeCodePoints() {
        assertThat(estimator.estimate("\u62db\u8058\u9762\u8bd5")).isEqualTo(4);
        assertThat(estimator.estimate("abcdefgh")).isEqualTo(2);

        String truncated = estimator.truncate("\u62db\u8058\ud83d\ude80\u9762\u8bd5abcdefgh", 5);

        assertThat(estimator.estimate(truncated)).isLessThanOrEqualTo(5);
        assertThat(truncated).doesNotContain("\ufffd");
    }
}
