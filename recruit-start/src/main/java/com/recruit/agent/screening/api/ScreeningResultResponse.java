package com.recruit.agent.screening.api;

import com.recruit.agent.screening.domain.ScreeningRecommendation;
import com.recruit.agent.screening.domain.ScreeningResult;

import java.util.List;

public record ScreeningResultResponse(
        ScreeningRecommendation recommendation,
        ScoreBreakdownResponse score,
        List<String> advantages,
        List<String> risks,
        List<String> missingInformation
) {

    public static ScreeningResultResponse from(ScreeningResult result) {
        return new ScreeningResultResponse(
                result.recommendation(),
                ScoreBreakdownResponse.from(result.score()),
                result.advantages(),
                result.risks(),
                result.missingInformation()
        );
    }
}
