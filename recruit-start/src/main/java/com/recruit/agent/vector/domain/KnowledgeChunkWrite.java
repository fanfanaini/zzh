package com.recruit.agent.vector.domain;

public record KnowledgeChunkWrite(
        Long documentId,
        Long chunkId,
        String sourceName,
        String content,
        float[] embedding
) {
    public KnowledgeChunkWrite {
        if (documentId == null || chunkId == null) {
            throw new IllegalArgumentException("Knowledge chunk IDs must not be null");
        }
        if (sourceName == null || sourceName.isBlank() || content == null || content.isBlank()) {
            throw new IllegalArgumentException("Knowledge chunk content must not be blank");
        }
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("Knowledge chunk embedding must not be empty");
        }
        sourceName = sourceName.trim();
        content = content.trim();
        embedding = embedding.clone();
    }

    @Override
    public float[] embedding() {
        return embedding.clone();
    }

    public KnowledgeChunk chunk() {
        return new KnowledgeChunk(documentId, chunkId, sourceName, content);
    }
}
