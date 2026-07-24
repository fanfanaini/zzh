package com.recruit.agent.evaluation.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record RetrievalRankingSample(
        String caseId,
        Set<Long> relevantChunkIds,
        Map<Long, Integer> relevanceGrades,
        List<Long> candidateChunkIds,
        List<Long> baselineChunkIds,
        List<Long> rerankedChunkIds,
        long rerankLatencyMillis,
        boolean degraded
) {
    public RetrievalRankingSample {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        relevantChunkIds = relevantChunkIds == null ? Set.of() : Set.copyOf(relevantChunkIds);
        relevanceGrades = relevanceGrades == null ? Map.of() : Map.copyOf(relevanceGrades);
        candidateChunkIds = candidateChunkIds == null ? List.of() : List.copyOf(candidateChunkIds);
        baselineChunkIds = baselineChunkIds == null ? List.of() : List.copyOf(baselineChunkIds);
        rerankedChunkIds = rerankedChunkIds == null ? List.of() : List.copyOf(rerankedChunkIds);
        if (rerankLatencyMillis < 0) {
            throw new IllegalArgumentException("rerankLatencyMillis must not be negative");
        }
    }
}
