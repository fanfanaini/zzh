package com.recruit.agent.evaluation.domain;

public record RetrievalWeakCase(
        String caseId,
        double baselineReciprocalRank,
        double rerankedReciprocalRank,
        double baselineNdcg,
        double rerankedNdcg,
        String reason
) {
}
