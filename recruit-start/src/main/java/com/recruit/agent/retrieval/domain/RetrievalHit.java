package com.recruit.agent.retrieval.domain;

public record RetrievalHit(
        Long documentId,
        Long chunkId,
        String sourceName,
        String content,
        double score
) {

    public RetrievalHit {
        if (documentId == null || chunkId == null) {
            throw new IllegalArgumentException("documentId and chunkId must not be null");
        }
        if (sourceName == null || content == null) {
            throw new IllegalArgumentException("sourceName and content must not be null");
        }
        if (!Double.isFinite(score)) {
            throw new IllegalArgumentException("score must be finite");
        }
    }
}
