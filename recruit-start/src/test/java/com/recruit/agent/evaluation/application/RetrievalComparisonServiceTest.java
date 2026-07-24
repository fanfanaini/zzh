package com.recruit.agent.evaluation.application;

import com.recruit.agent.evaluation.domain.RetrievalRankingSample;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalComparisonServiceTest {

    @Test
    void provesRankingImprovementWithoutChangingCandidateRecall() {
        RetrievalRankingSample sample = new RetrievalRankingSample(
                "case-1",
                Set.of(3L),
                Map.of(3L, 3),
                List.of(1L, 2L, 3L),
                List.of(1L, 2L, 3L),
                List.of(3L, 1L, 2L),
                12,
                false
        );

        var report = new RetrievalComparisonService().compare(List.of(sample), 3);

        assertThat(report.candidateRecall()).isEqualTo(1);
        assertThat(report.baseline().recallAtK()).isEqualTo(1);
        assertThat(report.reranked().recallAtK()).isEqualTo(1);
        assertThat(report.reranked().mrrAtK()).isGreaterThan(report.baseline().mrrAtK());
        assertThat(report.reranked().ndcgAtK()).isGreaterThan(report.baseline().ndcgAtK());
        assertThat(report.averageRerankLatencyMillis()).isEqualTo(12);
        assertThat(report.degradedRate()).isZero();
    }

    @Test
    void reportsDegradedRateAcrossSamples() {
        RetrievalRankingSample healthy = sample("healthy", false);
        RetrievalRankingSample degraded = sample("degraded", true);

        var report = new RetrievalComparisonService().compare(List.of(healthy, degraded), 1);

        assertThat(report.degradedRate()).isEqualTo(0.5);
    }

    private RetrievalRankingSample sample(String id, boolean degraded) {
        return new RetrievalRankingSample(
                id, Set.of(1L), Map.of(), List.of(1L), List.of(1L), List.of(1L), 0, degraded);
    }
}
