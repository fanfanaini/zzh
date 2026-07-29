package com.recruit.agent.memory.api;

import com.recruit.agent.memory.domain.CandidateMemoryFact;
import com.recruit.agent.memory.domain.CandidateMemorySourceType;
import com.recruit.agent.memory.domain.CandidateMemoryType;

import java.time.Instant;

public record CandidateMemoryResponse(
        Long id,
        Long candidateId,
        CandidateMemoryType type,
        String factKey,
        String factValue,
        double confidence,
        CandidateMemorySourceType sourceType,
        Long sourceId,
        String evidence,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static CandidateMemoryResponse from(CandidateMemoryFact fact) {
        return new CandidateMemoryResponse(fact.id(), fact.candidateId(), fact.type(), fact.factKey(),
                fact.factValue(), fact.confidence(), fact.sourceType(), fact.sourceId(), fact.evidence(),
                fact.active(), fact.createdAt(), fact.updatedAt());
    }
}
