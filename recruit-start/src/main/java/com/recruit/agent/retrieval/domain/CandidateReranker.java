package com.recruit.agent.retrieval.domain;

import java.util.List;

public interface CandidateReranker {
    RerankOutcome rerank(String query, List<RankedRetrievalResult> candidates, int limit);
}
