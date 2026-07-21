package com.recruit.agent.evaluation.domain;

public record BinaryClassificationMetrics(
        long evaluatedCases,
        long truePositives,
        long falsePositives,
        long falseNegatives,
        long trueNegatives,
        double precision,
        double recall,
        double f1
) {
}
