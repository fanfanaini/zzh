package com.recruit.agent.vector.application;

public record RegisterEmbeddingVersionCommand(
        String versionId,
        String modelName,
        int dimensions,
        String notes
) {
}
