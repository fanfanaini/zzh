package com.recruit.agent.screening.domain;

import java.util.List;

public record ScreeningResult(
        ScreeningRecommendation recommendation,
        ScoreBreakdown score,
        List<String> advantages,
        List<String> risks,
        List<String> missingInformation
) {
}
