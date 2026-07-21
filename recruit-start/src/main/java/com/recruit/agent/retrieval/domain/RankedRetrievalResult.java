package com.recruit.agent.retrieval.domain;

public record RankedRetrievalResult(
        Long documentId,
        Long chunkId,
        String sourceName,
        String content,
        Integer denseRank,
        Double denseScore,
        Integer sparseRank,
        Double sparseScore,
        double fusedScore
) {
}
