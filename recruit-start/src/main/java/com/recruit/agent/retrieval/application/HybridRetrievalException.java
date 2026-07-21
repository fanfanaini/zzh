package com.recruit.agent.retrieval.application;

import com.recruit.agent.retrieval.domain.HybridRetrievalOutcome;

public class HybridRetrievalException extends IllegalStateException {
    private final HybridRetrievalOutcome outcome;

    public HybridRetrievalException(HybridRetrievalOutcome outcome) {
        super("All retrieval channels failed");
        this.outcome = outcome;
    }

    public HybridRetrievalOutcome outcome() {
        return outcome;
    }
}
