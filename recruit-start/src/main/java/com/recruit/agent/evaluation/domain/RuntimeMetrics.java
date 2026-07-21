package com.recruit.agent.evaluation.domain;

public record RuntimeMetrics(
        long latencySamples,
        double averageLatencyMillis,
        double p50LatencyMillis,
        double p95LatencyMillis,
        long promptTokens,
        long completionTokens,
        long totalTokens
) {
}
