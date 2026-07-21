package com.recruit.agent.screening.api;

import com.recruit.agent.screening.domain.Evaluation;
import com.recruit.agent.screening.domain.ScreeningRecommendation;

import java.time.Instant;
import java.util.List;

public record EvaluationResponse(
        Long id,
        Long jobId,
        Long candidateId,
        Double ruleScore,
        Double semanticScore,
        Double finalScore,
        ScreeningRecommendation recommendation,
        Double confidence,
        List<String> advantages,
        List<String> risks,
        List<String> missingInformation,
        String modelName,
        String promptVersion,
        String humanResult,
        Instant createdAt
) {

    public static EvaluationResponse from(Evaluation evaluation) {
        return new EvaluationResponse(
                evaluation.id(),
                evaluation.jobId(),
                evaluation.candidateId(),
                evaluation.ruleScore(),
                evaluation.semanticScore(),
                evaluation.finalScore(),
                evaluation.recommendation(),
                evaluation.confidence(),
                evaluation.advantages(),
                evaluation.risks(),
                evaluation.missingInformation(),
                evaluation.modelName(),
                evaluation.promptVersion(),
                evaluation.humanResult(),
                evaluation.createdAt()
        );
    }
}
