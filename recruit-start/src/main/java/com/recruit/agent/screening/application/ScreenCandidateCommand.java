package com.recruit.agent.screening.application;

public record ScreenCandidateCommand(
        Long jobId,
        Long candidateId
) {
}
