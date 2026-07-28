package com.recruit.agent.vector.infrastructure;

import com.recruit.agent.vector.domain.EmbeddingIndexRebuildJob;
import com.recruit.agent.vector.domain.EmbeddingModelVersion;
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
    private final List<KnowledgeVersionEmbedding> versionedKnowledgeChunks = new CopyOnWriteArrayList<>();
    private final List<EmbeddingModelVersion> modelVersions = new CopyOnWriteArrayList<>();
    private final List<EmbeddingIndexRebuildJob> rebuildJobs = new CopyOnWriteArrayList<>();

    public InMemoryVectorSearchRepository() {
        modelVersions.add(new EmbeddingModelVersion(
                "legacy-default", "legacy-default", 1536, "ACTIVE", true,
                Instant.EPOCH, Instant.EPOCH, "Default in-memory embedding version"));
    }

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
        KnowledgeEmbedding active = new KnowledgeEmbedding(documentId, chunkId, sourceName, content,
                embedding.clone(), Instant.now());
        knowledgeChunks.add(active);
        upsertVersion(activeKnowledgeEmbeddingVersion(), List.of(active));
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
        upsertVersion(activeKnowledgeEmbeddingVersion(), replacements);
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
        versionedKnowledgeChunks.removeIf(chunk -> chunk.documentId().equals(documentId));
        return knowledgeChunks.removeIf(chunk -> chunk.documentId().equals(documentId));
    }

    @Override
    public String activeKnowledgeEmbeddingVersion() {
        return modelVersions.stream()
                .filter(EmbeddingModelVersion::active)
                .map(EmbeddingModelVersion::versionId)
                .findFirst()
                .orElse("legacy-default");
    }

    @Override
    public List<EmbeddingModelVersion> listEmbeddingModelVersions() {
        return List.copyOf(modelVersions);
    }

    @Override
    public EmbeddingModelVersion registerEmbeddingModelVersion(
            String versionId,
            String modelName,
            int dimensions,
            String notes
    ) {
        if (modelVersions.stream().anyMatch(version -> version.versionId().equals(versionId))) {
            throw new IllegalArgumentException("Embedding model version already exists: " + versionId);
        }
        EmbeddingModelVersion version = new EmbeddingModelVersion(
                versionId, modelName, dimensions, "READY", false, Instant.now(), null, notes);
        modelVersions.add(version);
        return version;
    }

    @Override
    public synchronized void saveKnowledgeVersionDocument(String modelVersion, List<KnowledgeChunkWrite> chunks) {
        List<KnowledgeEmbedding> replacements = chunks.stream()
                .map(chunk -> new KnowledgeEmbedding(
                        chunk.documentId(), chunk.chunkId(), chunk.sourceName(), chunk.content(),
                        chunk.embedding(), Instant.now()))
                .toList();
        upsertVersion(modelVersion, replacements);
    }

    @Override
    public synchronized void activateKnowledgeEmbeddingVersion(String versionId) {
        List<KnowledgeVersionEmbedding> shadow = versionedKnowledgeChunks.stream()
                .filter(chunk -> chunk.modelVersion().equals(versionId))
                .toList();
        if (shadow.isEmpty() && !knowledgeChunks.isEmpty()) {
            throw new IllegalStateException("Embedding version has no rebuilt chunks: " + versionId);
        }
        knowledgeChunks.clear();
        knowledgeChunks.addAll(shadow.stream()
                .map(chunk -> new KnowledgeEmbedding(chunk.documentId(), chunk.chunkId(), chunk.sourceName(),
                        chunk.content(), chunk.embedding(), Instant.now()))
                .toList());
        Instant activatedAt = Instant.now();
        List<EmbeddingModelVersion> replacements = modelVersions.stream()
                .map(version -> new EmbeddingModelVersion(version.versionId(), version.modelName(),
                        version.dimensions(), version.versionId().equals(versionId) ? "ACTIVE" : "READY",
                        version.versionId().equals(versionId), version.createdAt(),
                        version.versionId().equals(versionId) ? activatedAt : version.activatedAt(),
                        version.notes()))
                .toList();
        modelVersions.clear();
        modelVersions.addAll(replacements);
    }

    @Override
    public EmbeddingIndexRebuildJob saveRebuildJob(EmbeddingIndexRebuildJob job) {
        rebuildJobs.removeIf(existing -> existing.jobId().equals(job.jobId()));
        rebuildJobs.add(job);
        return job;
    }

    @Override
    public List<EmbeddingIndexRebuildJob> listRebuildJobs() {
        return rebuildJobs.stream()
                .sorted(java.util.Comparator.comparing(EmbeddingIndexRebuildJob::startedAt).reversed())
                .toList();
    }

    private void upsertVersion(String modelVersion, List<KnowledgeEmbedding> chunks) {
        for (KnowledgeEmbedding chunk : chunks) {
            versionedKnowledgeChunks.removeIf(existing -> existing.modelVersion().equals(modelVersion)
                    && existing.chunkId().equals(chunk.chunkId()));
            versionedKnowledgeChunks.add(new KnowledgeVersionEmbedding(modelVersion, chunk.documentId(),
                    chunk.chunkId(), chunk.sourceName(), chunk.content(), chunk.embedding(), Instant.now()));
        }
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

    private record KnowledgeVersionEmbedding(String modelVersion, Long documentId, Long chunkId, String sourceName,
                                             String content, float[] embedding, Instant createdAt) {}
}
