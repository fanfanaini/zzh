package com.recruit.agent.jobapplication.api;

import com.recruit.agent.jobapplication.domain.JobApplicationStage;
import jakarta.validation.constraints.NotNull;

public record AdvanceJobApplicationRequest(
        @NotNull(message = "招聘阶段不能为空")
        JobApplicationStage stage,
        Long operatorUserId,
        String reason,
        Long evaluationId,
        Long conversationId,
        String note
) {
}
