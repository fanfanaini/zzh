package com.recruit.agent.screening.api;

import com.recruit.agent.screening.domain.Evaluation;
import com.recruit.agent.screening.domain.ScreeningRecommendation;

public record CandidateRankingResponse(
        Long evaluationId,
        Long candidateId,
        Double finalScore,
        ScreeningRecommendation recommendation,
        String humanResult
) {
    public static CandidateRankingResponse from(Evaluation evaluation) {
        return new CandidateRankingResponse(evaluation.id(), evaluation.candidateId(), evaluation.finalScore(),
                evaluation.recommendation(), evaluation.humanResult());
    }
}
