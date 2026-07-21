package com.recruit.agent.vector.api;

import com.recruit.agent.vector.domain.ProjectSimilarity;

public record ProjectSimilarityResponse(
        Long candidateId,
        String content,
        double score
) {

    public static ProjectSimilarityResponse from(ProjectSimilarity similarity) {
        return new ProjectSimilarityResponse(
                similarity.candidateId(),
                similarity.content(),
                similarity.score()
        );
    }
}
