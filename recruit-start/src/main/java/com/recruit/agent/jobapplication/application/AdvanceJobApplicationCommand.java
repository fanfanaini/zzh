package com.recruit.agent.jobapplication.application;

import com.recruit.agent.jobapplication.domain.JobApplicationStage;

public record AdvanceJobApplicationCommand(
        Long applicationId,
        JobApplicationStage stage,
        Long operatorUserId,
        String reason,
        Long evaluationId,
        Long conversationId,
        String note
) {
}
