package com.recruit.agent.vector.api;

import com.recruit.agent.vector.domain.EmbeddingModelVersion;

import java.time.Instant;

public record EmbeddingVersionResponse(
        String versionId,
        String modelName,
        int dimensions,
        String status,
        boolean active,
        Instant createdAt,
        Instant activatedAt,
        String notes
) {
    public static EmbeddingVersionResponse from(EmbeddingModelVersion version) {
        return new EmbeddingVersionResponse(version.versionId(), version.modelName(), version.dimensions(),
                version.status(), version.active(), version.createdAt(), version.activatedAt(), version.notes());
    }
}
