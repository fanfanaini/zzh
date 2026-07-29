package com.recruit.agent.jobapplication.api;

import com.recruit.agent.jobapplication.domain.JobApplication;
import com.recruit.agent.jobapplication.domain.JobApplicationStage;

import java.time.Instant;

public record JobApplicationResponse(
        Long id,
        Long jobId,
        Long candidateId,
        JobApplicationStage stage,
        Long ownerUserId,
        String source,
        Long latestEvaluationId,
        Long latestConversationId,
        String rejectionReason,
        String note,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt
) {
    public static JobApplicationResponse from(JobApplication application) {
        return new JobApplicationResponse(
                application.id(),
                application.jobId(),
                application.candidateId(),
                application.stage(),
                application.ownerUserId(),
                application.source(),
                application.latestEvaluationId(),
                application.latestConversationId(),
                application.rejectionReason(),
                application.note(),
                application.createdAt(),
                application.updatedAt(),
                application.closedAt()
        );
    }
}
