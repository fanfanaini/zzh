package com.recruit.agent.screening.api;

import jakarta.validation.constraints.NotNull;

public record ScreenCandidateRequest(
        @NotNull(message = "候选人 ID 不能为空")
        Long candidateId
) {
}
