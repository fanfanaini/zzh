package com.recruit.agent.retrieval.domain;

import java.util.List;

@FunctionalInterface
public interface DenseRetriever {
    List<RetrievalHit> search(float[] queryEmbedding, RetrievalRequest request);
}
