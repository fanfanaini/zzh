package com.recruit.agent.vector.domain;

import java.util.Optional;
import java.util.List;

public interface VectorSearchRepository {

    void saveJobDescription(Long jobId, String content, float[] embedding);

    void saveResumeProject(Long candidateId, Long projectId, String content, float[] embedding);

    Optional<ProjectSimilarity> findBestProjectSimilarity(Long jobId, Long candidateId, float[] queryEmbedding);

    void saveKnowledgeChunk(Long documentId, Long chunkId, String sourceName, String content, float[] embedding);

    default void saveKnowledgeDocument(List<KnowledgeChunkWrite> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("Knowledge document chunks must not be empty");
        }
        chunks.forEach(chunk -> saveKnowledgeChunk(
                chunk.documentId(),
                chunk.chunkId(),
                chunk.sourceName(),
                chunk.content(),
                chunk.embedding()
        ));
    }

    List<KnowledgeChunkMatch> searchKnowledge(float[] queryEmbedding, int limit);

    List<KnowledgeDocumentSummary> listKnowledgeDocuments();

    List<KnowledgeChunk> listKnowledgeChunks();

    boolean deleteKnowledgeDocument(Long documentId);

    default String activeKnowledgeEmbeddingVersion() {
        return "legacy-default";
    }

    default List<EmbeddingModelVersion> listEmbeddingModelVersions() {
        return List.of();
    }

    default EmbeddingModelVersion registerEmbeddingModelVersion(
            String versionId,
            String modelName,
            int dimensions,
            String notes
    ) {
        throw new UnsupportedOperationException("Embedding model version registry is not available");
    }

    default void saveKnowledgeVersionDocument(String modelVersion, List<KnowledgeChunkWrite> chunks) {
        saveKnowledgeDocument(chunks);
    }

    default void activateKnowledgeEmbeddingVersion(String versionId) {
        throw new UnsupportedOperationException("Embedding model version activation is not available");
    }

    default EmbeddingIndexRebuildJob saveRebuildJob(EmbeddingIndexRebuildJob job) {
        return job;
    }

    default List<EmbeddingIndexRebuildJob> listRebuildJobs() {
        return List.of();
    }
}
