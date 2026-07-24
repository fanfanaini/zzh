package com.recruit.agent.retrieval.domain;

import java.util.List;

public record RerankOutcome(
        List<RankedRetrievalResult> results,
        String strategy,
        boolean degraded
) {
    public RerankOutcome {
        results = results == null ? List.of() : List.copyOf(results);
        strategy = strategy == null || strategy.isBlank() ? "unknown" : strategy;
    }
}
