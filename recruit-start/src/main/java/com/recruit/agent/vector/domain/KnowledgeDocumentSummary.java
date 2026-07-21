package com.recruit.agent.vector.domain;

import java.time.Instant;

public record KnowledgeDocumentSummary(
        Long documentId,
        String sourceName,
        int chunkCount,
        Instant createdAt
) {
}
