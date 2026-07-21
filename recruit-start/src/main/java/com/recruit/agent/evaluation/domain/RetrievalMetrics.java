package com.recruit.agent.evaluation.domain;

public record RetrievalMetrics(int k, long evaluatedCases, double recallAtK, double meanReciprocalRank) {
}
