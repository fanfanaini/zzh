package com.recruit.agent.vector.domain;

public record ProjectSimilarity(
        Long candidateId,
        String content,
        double score
) {
}
