package com.recruit.agent.jobapplication.application;

public record CreateJobApplicationCommand(
        Long jobId,
        Long candidateId,
        Long ownerUserId,
        String source,
        String note
) {
}
