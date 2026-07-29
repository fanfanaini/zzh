package com.recruit.agent.jobapplication.api;

import jakarta.validation.constraints.NotNull;

public record CreateJobApplicationRequest(
        @NotNull(message = "岗位 ID 不能为空")
        Long jobId,
        @NotNull(message = "候选人 ID 不能为空")
        Long candidateId,
        Long ownerUserId,
        String source,
        String note
) {
}
