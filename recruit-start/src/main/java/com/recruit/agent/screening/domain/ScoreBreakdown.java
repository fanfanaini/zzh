package com.recruit.agent.screening.domain;

public record ScoreBreakdown(
        double skillScore,
        double projectScore,
        double experienceScore,
        double educationScore,
        double bonusScore,
        double finalScore
) {
}
