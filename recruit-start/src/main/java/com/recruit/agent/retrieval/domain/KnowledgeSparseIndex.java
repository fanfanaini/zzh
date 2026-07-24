package com.recruit.agent.retrieval.domain;

import com.recruit.agent.vector.domain.KnowledgeChunk;

import java.util.List;

public interface KnowledgeSparseIndex extends SparseRetriever {
    void rebuild(List<KnowledgeChunk> chunks);

    void upsertDocument(List<KnowledgeChunk> chunks);

    void deleteDocument(Long documentId);
}
