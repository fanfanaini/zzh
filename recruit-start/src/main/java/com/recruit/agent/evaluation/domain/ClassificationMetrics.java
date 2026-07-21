package com.recruit.agent.evaluation.domain;

public record ClassificationMetrics(long evaluatedCases, double accuracy, double macroF1) {
}
