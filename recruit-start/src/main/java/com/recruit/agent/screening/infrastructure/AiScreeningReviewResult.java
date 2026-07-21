package com.recruit.agent.screening.infrastructure;

import java.util.List;

public record AiScreeningReviewResult(
        String recommendation,
        double confidence,
        List<String> advantages,
        List<String> risks,
        List<String> missingInformation,
        List<String> suggestedQuestions
) {
}
