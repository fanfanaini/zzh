package com.recruit.agent.vector.domain;

public record KnowledgeChunk(
        Long documentId,
        Long chunkId,
        String sourceName,
        String content
) {
    public KnowledgeChunk {
        if (documentId == null || chunkId == null) {
            throw new IllegalArgumentException("Knowledge chunk IDs must not be null");
        }
        sourceName = sourceName == null ? "" : sourceName;
        content = content == null ? "" : content;
    }
}
