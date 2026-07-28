package com.recruit.agent.vector.domain;

import java.time.Instant;

public record EmbeddingModelVersion(
        String versionId,
        String modelName,
        int dimensions,
        String status,
        boolean active,
        Instant createdAt,
        Instant activatedAt,
        String notes
) {
}
