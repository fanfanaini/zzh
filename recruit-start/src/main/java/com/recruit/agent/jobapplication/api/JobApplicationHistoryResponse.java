package com.recruit.agent.jobapplication.api;

import com.recruit.agent.jobapplication.domain.JobApplicationHistory;
import com.recruit.agent.jobapplication.domain.JobApplicationStage;

import java.time.Instant;

public record JobApplicationHistoryResponse(
        Long id,
        Long applicationId,
        JobApplicationStage fromStage,
        JobApplicationStage toStage,
        Long operatorUserId,
        String reason,
        Instant createdAt
) {
    public static JobApplicationHistoryResponse from(JobApplicationHistory history) {
        return new JobApplicationHistoryResponse(
                history.id(),
                history.applicationId(),
                history.fromStage(),
                history.toStage(),
                history.operatorUserId(),
                history.reason(),
                history.createdAt()
        );
    }
}
