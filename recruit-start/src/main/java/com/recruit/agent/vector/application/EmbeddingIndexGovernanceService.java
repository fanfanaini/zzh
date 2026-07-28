package com.recruit.agent.vector.application;

import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.vector.domain.EmbeddingIndexRebuildJob;
import com.recruit.agent.vector.domain.EmbeddingModelVersion;
import com.recruit.agent.vector.domain.EmbeddingService;
import com.recruit.agent.vector.domain.KnowledgeChunk;
import com.recruit.agent.vector.domain.KnowledgeChunkWrite;
import com.recruit.agent.vector.domain.VectorSearchRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class EmbeddingIndexGovernanceService {
    private final EmbeddingService embeddingService;
    private final VectorSearchRepository repository;
    private final TimestampIdGenerator ids = new TimestampIdGenerator();

    public EmbeddingIndexGovernanceService(EmbeddingService embeddingService, VectorSearchRepository repository) {
        this.embeddingService = embeddingService;
        this.repository = repository;
    }

    public List<EmbeddingModelVersion> listVersions() {
        return repository.listEmbeddingModelVersions();
    }

    public String activeVersion() {
        return repository.activeKnowledgeEmbeddingVersion();
    }

    public EmbeddingModelVersion register(RegisterEmbeddingVersionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Embedding version command must not be null");
        }
        String versionId = normalize(command.versionId(), "versionId");
        String modelName = normalize(command.modelName(), "modelName");
        if (command.dimensions() <= 0) {
            throw new IllegalArgumentException("dimensions must be positive");
        }
        return repository.registerEmbeddingModelVersion(versionId, modelName, command.dimensions(), command.notes());
    }

    public EmbeddingIndexRebuildJob rebuildKnowledgeIndex(String targetVersion) {
        String version = normalize(targetVersion, "targetVersion");
        ensureVersionExists(version);
        List<KnowledgeChunk> chunks = repository.listKnowledgeChunks();
        String jobId = String.valueOf(ids.nextId());
        Instant started = Instant.now();
        repository.saveRebuildJob(new EmbeddingIndexRebuildJob(
                jobId, version, "RUNNING", chunks.size(), 0, 0, started, null, null));
        try {
            List<KnowledgeChunkWrite> writes = new ArrayList<>(chunks.size());
            for (KnowledgeChunk chunk : chunks) {
                writes.add(new KnowledgeChunkWrite(
                        chunk.documentId(),
                        chunk.chunkId(),
                        chunk.sourceName(),
                        chunk.content(),
                        embeddingService.embed(chunk.content())
                ));
            }
            if (!writes.isEmpty()) {
                repository.saveKnowledgeVersionDocument(version, writes);
            }
            EmbeddingIndexRebuildJob finished = new EmbeddingIndexRebuildJob(
                    jobId, version, "SUCCEEDED", chunks.size(), chunks.size(), 0, started, Instant.now(), null);
            return repository.saveRebuildJob(finished);
        } catch (RuntimeException exception) {
            EmbeddingIndexRebuildJob failed = new EmbeddingIndexRebuildJob(
                    jobId, version, "FAILED", chunks.size(), 0, chunks.size(), started, Instant.now(),
                    exception.getMessage());
            repository.saveRebuildJob(failed);
            throw exception;
        }
    }

    public void activate(String versionId) {
        String version = normalize(versionId, "versionId");
        ensureVersionExists(version);
        repository.activateKnowledgeEmbeddingVersion(version);
    }

    public void rollback(String versionId) {
        activate(versionId);
    }

    public List<EmbeddingIndexRebuildJob> listRebuildJobs() {
        return repository.listRebuildJobs();
    }

    private void ensureVersionExists(String versionId) {
        boolean exists = repository.listEmbeddingModelVersions().stream()
                .anyMatch(version -> version.versionId().equals(versionId));
        if (!exists) {
            throw new NoSuchElementException("Embedding model version does not exist: " + versionId);
        }
    }

    private String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
