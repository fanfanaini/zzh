package com.recruit.agent.retrieval.domain;

public record RetrievalRequest(
        int limit,
        int candidateLimit,
        double minDenseScore,
        RetrievalFilters filters
) {

    public RetrievalRequest {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
        if (candidateLimit < limit || candidateLimit > 100) {
            throw new IllegalArgumentException("candidateLimit must be between limit and 100");
        }
        if (minDenseScore < 0 || minDenseScore > 100) {
            throw new IllegalArgumentException("minDenseScore must be between 0 and 100");
        }
        filters = filters == null ? RetrievalFilters.none() : filters;
    }

    public static RetrievalRequest of(int limit, double minDenseScore) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        return new RetrievalRequest(safeLimit, Math.min(safeLimit * 3, 60), minDenseScore,
                RetrievalFilters.none());
    }
}
