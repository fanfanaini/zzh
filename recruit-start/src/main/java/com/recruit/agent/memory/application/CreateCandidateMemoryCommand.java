package com.recruit.agent.memory.application;

import com.recruit.agent.memory.domain.CandidateMemorySourceType;
import com.recruit.agent.memory.domain.CandidateMemoryType;

public record CreateCandidateMemoryCommand(
        Long candidateId,
        CandidateMemoryType type,
        String factKey,
        String factValue,
        Double confidence,
        CandidateMemorySourceType sourceType,
        Long sourceId,
        String evidence
) {
}
