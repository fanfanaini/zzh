package com.recruit.agent.retrieval.domain;

import java.util.List;

@FunctionalInterface
public interface SparseRetriever {
    List<RetrievalHit> search(String query, RetrievalRequest request);
}
