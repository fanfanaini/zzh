package com.recruit.agent.vector.domain;

import java.util.Optional;
import java.util.List;

public interface VectorSearchRepository {

    void saveJobDescription(Long jobId, String content, float[] embedding);

    void saveResumeProject(Long candidateId, Long projectId, String content, float[] embedding);

    Optional<ProjectSimilarity> findBestProjectSimilarity(Long jobId, Long candidateId, float[] queryEmbedding);

    void saveKnowledgeChunk(Long documentId, Long chunkId, String sourceName, String content, float[] embedding);

    List<KnowledgeChunkMatch> searchKnowledge(float[] queryEmbedding, int limit);

    List<KnowledgeDocumentSummary> listKnowledgeDocuments();

    boolean deleteKnowledgeDocument(Long documentId);
}
