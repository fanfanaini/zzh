package com.recruit.agent.evaluation.domain;

public record EndToEndMetrics(
        long evaluatedCases,
        long successfulCases,
        double successRate,
        double expectedOutcomeMatchRate
) {
}
