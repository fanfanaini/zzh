package com.recruit.agent.screening.domain;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.job.domain.Job;

public record ScreeningReviewRequest(
        Job job,
        Candidate candidate,
        ScreeningResult ruleResult,
        Double semanticScore
) {
}
