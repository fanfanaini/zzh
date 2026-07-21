package com.recruit.agent.retrieval.domain;

import java.util.List;

public record HybridRetrievalOutcome(
        List<RankedRetrievalResult> results,
        RetrievalChannelDiagnostic dense,
        RetrievalChannelDiagnostic sparse
) {
    public HybridRetrievalOutcome {
        results = results == null ? List.of() : List.copyOf(results);
        if (dense == null || sparse == null) {
            throw new IllegalArgumentException("Retrieval diagnostics must not be null");
        }
    }

    public boolean degraded() {
        return !dense.succeeded() || !sparse.succeeded();
    }

    public boolean allChannelsFailed() {
        return !dense.succeeded() && !sparse.succeeded();
    }
}
