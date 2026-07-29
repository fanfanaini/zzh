package com.recruit.agent.memory.api;

import com.recruit.agent.memory.domain.CandidateMemoryType;
import jakarta.validation.constraints.NotBlank;

public record CreateCandidateMemoryRequest(
        CandidateMemoryType type,
        @NotBlank String factKey,
        @NotBlank String factValue,
        Double confidence,
        String evidence
) {
}
