package com.recruit.agent.evaluation.domain;

public record RetrievalMetricDelta(
        double recallAtK,
        double mrrAtK,
        double ndcgAtK,
        double hitRateAtK
) {
    public static RetrievalMetricDelta between(RankingMetrics baseline, RankingMetrics reranked) {
        return new RetrievalMetricDelta(
                reranked.recallAtK() - baseline.recallAtK(),
                reranked.mrrAtK() - baseline.mrrAtK(),
                reranked.ndcgAtK() - baseline.ndcgAtK(),
                reranked.hitRateAtK() - baseline.hitRateAtK()
        );
    }
}
