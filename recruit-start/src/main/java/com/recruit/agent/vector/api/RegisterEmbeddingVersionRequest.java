package com.recruit.agent.vector.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RegisterEmbeddingVersionRequest(
        @NotBlank String versionId,
        @NotBlank String modelName,
        @Positive int dimensions,
        String notes
) {
}
