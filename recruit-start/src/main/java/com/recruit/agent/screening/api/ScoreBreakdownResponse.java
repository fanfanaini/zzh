package com.recruit.agent.screening.api;

import com.recruit.agent.screening.domain.ScoreBreakdown;

public record ScoreBreakdownResponse(
        double skillScore,
        double projectScore,
        double experienceScore,
        double educationScore,
        double bonusScore,
        double finalScore
) {

    public static ScoreBreakdownResponse from(ScoreBreakdown score) {
        return new ScoreBreakdownResponse(
                score.skillScore(),
                score.projectScore(),
                score.experienceScore(),
                score.educationScore(),
                score.bonusScore(),
                score.finalScore()
        );
    }
}
