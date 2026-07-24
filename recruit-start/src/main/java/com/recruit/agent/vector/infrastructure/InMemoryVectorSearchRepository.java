package com.recruit.agent.vector.infrastructure;

import com.recruit.agent.vector.domain.ProjectSimilarity;
import com.recruit.agent.vector.domain.VectorSearchRepository;
import com.recruit.agent.vector.domain.KnowledgeChunkMatch;
import com.recruit.agent.vector.domain.KnowledgeDocumentSummary;
import com.recruit.agent.vector.domain.KnowledgeChunk;
import com.recruit.agent.vector.domain.KnowledgeChunkWrite;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.Instant;

@Repository
@Profile("!vector")
public class InMemoryVectorSearchRepository implements VectorSearchRepository {

    private final List<ResumeProjectEmbedding> resumeProjects = new CopyOnWriteArrayList<>();
    private final List<KnowledgeEmbedding> knowledgeChunks = new CopyOnWriteArrayList<>();

    @Override
    public void saveJobDescription(Long jobId, String content, float[] embedding) {
    }

    @Override
    public void saveResumeProject(Long candidateId, Long projectId, String content, float[] embedding) {
        resumeProjects.add(new ResumeProjectEmbedding(candidateId, content, embedding));
    }

    @Override
    public Optional<ProjectSimilarity> findBestProjectSimilarity(Long jobId, Long candidateId, float[] queryEmbedding) {
        List<ProjectSimilarity> similarities = new ArrayList<>();
        for (ResumeProjectEmbedding project : resumeProjects) {
            if (project.candidateId().equals(candidateId)) {
                similarities.add(new ProjectSimilarity(candidateId, project.content(), cosine(queryEmbedding, project.embedding()) * 100));
            }
        }
        return similarities.stream()
                .max(java.util.Comparator.comparing(ProjectSimilarity::score));
    }

    @Override
    public void saveKnowledgeChunk(Long documentId, Long chunkId, String sourceName, String content, float[] embedding) {
        knowledgeChunks.removeIf(chunk -> chunk.chunkId().equals(chunkId));
        knowledgeChunks.add(new KnowledgeEmbedding(documentId, chunkId, sourceName, content, embedding.clone(), Instant.now()));
    }

    @Override
    public synchronized void saveKnowledgeDocument(List<KnowledgeChunkWrite> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("Knowledge document chunks must not be empty");
        }
        Long documentId = chunks.get(0).documentId();
        List<KnowledgeEmbedding> replacements = chunks.stream()
                .map(chunk -> new KnowledgeEmbedding(
                        chunk.documentId(), chunk.chunkId(), chunk.sourceName(), chunk.content(),
                        chunk.embedding(), Instant.now()))
                .toList();
        knowledgeChunks.removeIf(chunk -> chunk.documentId().equals(documentId));
        knowledgeChunks.addAll(replacements);
    }

    @Override
    public List<KnowledgeChunkMatch> searchKnowledge(float[] queryEmbedding, int limit) {
        return knowledgeChunks.stream()
                .map(chunk -> new KnowledgeChunkMatch(chunk.documentId(), chunk.chunkId(), chunk.sourceName(),
                        chunk.content(), Math.round(cosine(queryEmbedding, chunk.embedding()) * 10000.0) / 100.0))
                .sorted(java.util.Comparator.comparing(KnowledgeChunkMatch::score).reversed())
                .limit(limit).toList();
    }

    @Override
    public List<KnowledgeDocumentSummary> listKnowledgeDocuments() {
        return knowledgeChunks.stream().collect(java.util.stream.Collectors.groupingBy(
                        KnowledgeEmbedding::documentId,
                        java.util.stream.Collectors.toList()))
                .values().stream()
                .map(chunks -> new KnowledgeDocumentSummary(chunks.get(0).documentId(), chunks.get(0).sourceName(),
                        chunks.size(), chunks.stream().map(KnowledgeEmbedding::createdAt).min(Instant::compareTo).orElse(Instant.now())))
                .sorted(java.util.Comparator.comparing(KnowledgeDocumentSummary::createdAt).reversed())
                .toList();
    }

    @Override
    public List<KnowledgeChunk> listKnowledgeChunks() {
        return knowledgeChunks.stream()
                .map(chunk -> new KnowledgeChunk(
                        chunk.documentId(), chunk.chunkId(), chunk.sourceName(), chunk.content()))
                .toList();
    }

    @Override
    public boolean deleteKnowledgeDocument(Long documentId) {
        return knowledgeChunks.removeIf(chunk -> chunk.documentId().equals(documentId));
    }

    private double cosine(float[] left, float[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private record ResumeProjectEmbedding(Long candidateId, String content, float[] embedding) {
    }

    private record KnowledgeEmbedding(Long documentId, Long chunkId, String sourceName, String content,
                                      float[] embedding, Instant createdAt) {}
}
