package com.recruit.agent.evaluation.domain;

public record RetrievalComparisonReport(
        int cases,
        int k,
        double candidateRecall,
        RankingMetrics baseline,
        RankingMetrics reranked,
        double averageRerankLatencyMillis,
        double degradedRate
) {
}
