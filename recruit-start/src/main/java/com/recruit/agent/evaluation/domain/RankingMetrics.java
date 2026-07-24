package com.recruit.agent.evaluation.domain;

public record RankingMetrics(
        double recallAtK,
        double mrrAtK,
        double ndcgAtK,
        double hitRateAtK
) {
}
