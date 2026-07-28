package com.recruit.agent.vector.infrastructure;

import com.recruit.agent.vector.domain.EmbeddingIndexRebuildJob;
import com.recruit.agent.vector.domain.EmbeddingModelVersion;
import com.recruit.agent.vector.domain.KnowledgeChunk;
import com.recruit.agent.vector.domain.KnowledgeChunkMatch;
import com.recruit.agent.vector.domain.KnowledgeChunkWrite;
import com.recruit.agent.vector.domain.KnowledgeDocumentSummary;
import com.recruit.agent.vector.domain.ProjectSimilarity;
import com.recruit.agent.vector.domain.VectorSearchRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("vector")
public class PgVectorSearchRepository implements VectorSearchRepository {

    private final VectorProperties properties;

    public PgVectorSearchRepository(VectorProperties properties) {
        this.properties = properties;
    }

    @Override
    public void saveJobDescription(Long jobId, String content, float[] embedding) {
        execute("""
                INSERT INTO job_description_embedding(job_id, content, embedding)
                VALUES (?, ?, ?::vector)
                """, jobId, content, toVectorLiteral(embedding));
    }

    @Override
    public void saveResumeProject(Long candidateId, Long projectId, String content, float[] embedding) {
        execute("""
                INSERT INTO resume_project_embedding(candidate_id, project_id, content, embedding)
                VALUES (?, ?, ?, ?::vector)
                """, candidateId, projectId, content, toVectorLiteral(embedding));
    }

    @Override
    public Optional<ProjectSimilarity> findBestProjectSimilarity(Long jobId, Long candidateId, float[] queryEmbedding) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT candidate_id, content, 1 - (embedding <=> ?::vector) AS similarity
                     FROM resume_project_embedding
                     WHERE candidate_id = ?
                     ORDER BY embedding <=> ?::vector
                     LIMIT 1
                     """)) {
            String vector = toVectorLiteral(queryEmbedding);
            statement.setString(1, vector);
            statement.setLong(2, candidateId);
            statement.setString(3, vector);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ProjectSimilarity(
                        resultSet.getLong("candidate_id"),
                        resultSet.getString("content"),
                        round(resultSet.getDouble("similarity") * 100)
                ));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector project similarity query failed", exception);
        }
    }

    @Override
    public void saveKnowledgeChunk(Long documentId, Long chunkId, String sourceName, String content, float[] embedding) {
        KnowledgeChunkWrite write = new KnowledgeChunkWrite(documentId, chunkId, sourceName, content, embedding);
        saveKnowledgeDocument(List.of(write));
    }

    @Override
    public void saveKnowledgeDocument(List<KnowledgeChunkWrite> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("Knowledge document chunks must not be empty");
        }
        String sql = """
                INSERT INTO knowledge_chunk_embedding(document_id, chunk_id, source_name, content, embedding)
                VALUES (?, ?, ?, ?, ?::vector)
                ON CONFLICT (chunk_id) DO UPDATE SET
                    document_id = EXCLUDED.document_id,
                    source_name = EXCLUDED.source_name,
                    content = EXCLUDED.content,
                    embedding = EXCLUDED.embedding
                """;
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            try {
                for (KnowledgeChunkWrite chunk : chunks) {
                    bindKnowledgeChunk(statement, chunk, 0);
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector knowledge document write failed", exception);
        }
        saveKnowledgeVersionDocument(activeKnowledgeEmbeddingVersion(), chunks);
    }

    @Override
    public List<KnowledgeChunkMatch> searchKnowledge(float[] queryEmbedding, int limit) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT document_id, chunk_id, source_name, content, 1 - (embedding <=> ?::vector) AS similarity
                     FROM knowledge_chunk_embedding
                     ORDER BY embedding <=> ?::vector
                     LIMIT ?
                     """)) {
            String vector = toVectorLiteral(queryEmbedding);
            statement.setString(1, vector);
            statement.setString(2, vector);
            statement.setInt(3, limit);
            List<KnowledgeChunkMatch> matches = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    matches.add(new KnowledgeChunkMatch(rs.getLong("document_id"), rs.getLong("chunk_id"),
                            rs.getString("source_name"), rs.getString("content"),
                            round(rs.getDouble("similarity") * 100)));
                }
            }
            return List.copyOf(matches);
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector knowledge search failed", exception);
        }
    }

    @Override
    public List<KnowledgeDocumentSummary> listKnowledgeDocuments() {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT document_id, MIN(source_name) AS source_name, COUNT(*) AS chunk_count,
                            MIN(created_at) AS created_at
                     FROM knowledge_chunk_embedding
                     GROUP BY document_id
                     ORDER BY MIN(created_at) DESC
                     """);
             ResultSet rs = statement.executeQuery()) {
            List<KnowledgeDocumentSummary> documents = new ArrayList<>();
            while (rs.next()) {
                documents.add(new KnowledgeDocumentSummary(rs.getLong("document_id"), rs.getString("source_name"),
                        rs.getInt("chunk_count"), rs.getTimestamp("created_at").toInstant()));
            }
            return List.copyOf(documents);
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector knowledge document query failed", exception);
        }
    }

    @Override
    public List<KnowledgeChunk> listKnowledgeChunks() {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT document_id, chunk_id, source_name, content
                     FROM knowledge_chunk_embedding
                     ORDER BY chunk_id ASC
                     """);
             ResultSet rs = statement.executeQuery()) {
            List<KnowledgeChunk> chunks = new ArrayList<>();
            while (rs.next()) {
                chunks.add(new KnowledgeChunk(
                        rs.getLong("document_id"),
                        rs.getLong("chunk_id"),
                        rs.getString("source_name"),
                        rs.getString("content")
                ));
            }
            return List.copyOf(chunks);
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector knowledge chunk query failed", exception);
        }
    }

    @Override
    public boolean deleteKnowledgeDocument(Long documentId) {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                boolean deleted;
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM knowledge_chunk_embedding WHERE document_id = ?")) {
                    statement.setLong(1, documentId);
                    deleted = statement.executeUpdate() > 0;
                }
                if (deleted) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "DELETE FROM knowledge_chunk_embedding_version WHERE document_id = ?")) {
                        statement.setLong(1, documentId);
                        statement.executeUpdate();
                    }
                }
                connection.commit();
                return deleted;
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector knowledge document delete failed", exception);
        }
    }

    @Override
    public String activeKnowledgeEmbeddingVersion() {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT version_id
                     FROM embedding_model_version
                     WHERE active = TRUE
                     LIMIT 1
                     """);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getString("version_id") : "legacy-default";
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector active embedding version query failed", exception);
        }
    }

    @Override
    public List<EmbeddingModelVersion> listEmbeddingModelVersions() {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT version_id, model_name, dimensions, status, active, created_at, activated_at, notes
                     FROM embedding_model_version
                     ORDER BY active DESC, created_at DESC
                     """);
             ResultSet rs = statement.executeQuery()) {
            List<EmbeddingModelVersion> versions = new ArrayList<>();
            while (rs.next()) {
                versions.add(toModelVersion(rs));
            }
            return List.copyOf(versions);
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector embedding version query failed", exception);
        }
    }

    @Override
    public EmbeddingModelVersion registerEmbeddingModelVersion(
            String versionId,
            String modelName,
            int dimensions,
            String notes
    ) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO embedding_model_version(version_id, model_name, dimensions, status, active, notes)
                     VALUES (?, ?, ?, 'READY', FALSE, ?)
                     """)) {
            statement.setString(1, versionId);
            statement.setString(2, modelName);
            statement.setInt(3, dimensions);
            statement.setString(4, notes);
            statement.executeUpdate();
            return listEmbeddingModelVersions().stream()
                    .filter(version -> version.versionId().equals(versionId))
                    .findFirst()
                    .orElseThrow();
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector embedding version registration failed", exception);
        }
    }

    @Override
    public void saveKnowledgeVersionDocument(String modelVersion, List<KnowledgeChunkWrite> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO knowledge_chunk_embedding_version(
                    model_version, document_id, chunk_id, source_name, content, embedding)
                VALUES (?, ?, ?, ?, ?, ?::vector)
                ON CONFLICT (model_version, chunk_id) DO UPDATE SET
                    document_id = EXCLUDED.document_id,
                    source_name = EXCLUDED.source_name,
                    content = EXCLUDED.content,
                    embedding = EXCLUDED.embedding
                """;
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            try {
                for (KnowledgeChunkWrite chunk : chunks) {
                    statement.setString(1, modelVersion);
                    bindKnowledgeChunk(statement, chunk, 1);
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector versioned knowledge write failed", exception);
        }
    }

    @Override
    public void activateKnowledgeEmbeddingVersion(String versionId) {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try {
                int activeChunkCount = countRows(connection, "knowledge_chunk_embedding", null);
                int versionedChunkCount = countRows(connection, "knowledge_chunk_embedding_version", versionId);
                if (versionedChunkCount == 0 && activeChunkCount > 0) {
                    throw new IllegalStateException("Embedding version has no rebuilt chunks: " + versionId);
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM knowledge_chunk_embedding")) {
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO knowledge_chunk_embedding(document_id, chunk_id, source_name, content, embedding)
                        SELECT document_id, chunk_id, source_name, content, embedding
                        FROM knowledge_chunk_embedding_version
                        WHERE model_version = ?
                        """)) {
                    statement.setString(1, versionId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE embedding_model_version
                        SET active = FALSE, status = CASE WHEN status = 'ACTIVE' THEN 'READY' ELSE status END
                        """)) {
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE embedding_model_version
                        SET active = TRUE, status = 'ACTIVE', activated_at = CURRENT_TIMESTAMP
                        WHERE version_id = ?
                        """)) {
                    statement.setString(1, versionId);
                    if (statement.executeUpdate() == 0) {
                        throw new IllegalStateException("Embedding model version does not exist: " + versionId);
                    }
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector embedding version activation failed", exception);
        }
    }

    @Override
    public EmbeddingIndexRebuildJob saveRebuildJob(EmbeddingIndexRebuildJob job) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO embedding_index_rebuild_job(
                         job_id, target_version, status, total_chunks, succeeded_chunks, failed_chunks,
                         started_at, finished_at, error_message)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                     ON CONFLICT (job_id) DO UPDATE SET
                         status = EXCLUDED.status,
                         total_chunks = EXCLUDED.total_chunks,
                         succeeded_chunks = EXCLUDED.succeeded_chunks,
                         failed_chunks = EXCLUDED.failed_chunks,
                         finished_at = EXCLUDED.finished_at,
                         error_message = EXCLUDED.error_message
                     """)) {
            statement.setString(1, job.jobId());
            statement.setString(2, job.targetVersion());
            statement.setString(3, job.status());
            statement.setInt(4, job.totalChunks());
            statement.setInt(5, job.succeededChunks());
            statement.setInt(6, job.failedChunks());
            statement.setTimestamp(7, Timestamp.from(job.startedAt()));
            statement.setTimestamp(8, job.finishedAt() == null ? null : Timestamp.from(job.finishedAt()));
            statement.setString(9, job.errorMessage());
            statement.executeUpdate();
            return job;
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector rebuild job save failed", exception);
        }
    }

    @Override
    public List<EmbeddingIndexRebuildJob> listRebuildJobs() {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT job_id, target_version, status, total_chunks, succeeded_chunks, failed_chunks,
                            started_at, finished_at, error_message
                     FROM embedding_index_rebuild_job
                     ORDER BY started_at DESC
                     LIMIT 50
                     """);
             ResultSet rs = statement.executeQuery()) {
            List<EmbeddingIndexRebuildJob> jobs = new ArrayList<>();
            while (rs.next()) {
                jobs.add(new EmbeddingIndexRebuildJob(
                        rs.getString("job_id"),
                        rs.getString("target_version"),
                        rs.getString("status"),
                        rs.getInt("total_chunks"),
                        rs.getInt("succeeded_chunks"),
                        rs.getInt("failed_chunks"),
                        rs.getTimestamp("started_at").toInstant(),
                        rs.getTimestamp("finished_at") == null ? null : rs.getTimestamp("finished_at").toInstant(),
                        rs.getString("error_message")
                ));
            }
            return List.copyOf(jobs);
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector rebuild job query failed", exception);
        }
    }

    private void execute(String sql, Object... args) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            statement.executeUpdate();
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector write failed", exception);
        }
    }

    private Connection connection() throws Exception {
        if (properties.getDriverClassName() != null && !properties.getDriverClassName().isBlank()) {
            Class.forName(properties.getDriverClassName());
        }
        return DriverManager.getConnection(properties.getUrl(), properties.getUsername(), properties.getPassword());
    }

    private void bindKnowledgeChunk(PreparedStatement statement, KnowledgeChunkWrite chunk, int offset)
            throws Exception {
        statement.setLong(offset + 1, chunk.documentId());
        statement.setLong(offset + 2, chunk.chunkId());
        statement.setString(offset + 3, chunk.sourceName());
        statement.setString(offset + 4, chunk.content());
        statement.setString(offset + 5, toVectorLiteral(chunk.embedding()));
    }

    private int countRows(Connection connection, String table, String modelVersion) throws Exception {
        String sql = modelVersion == null
                ? "SELECT COUNT(*) FROM " + table
                : "SELECT COUNT(*) FROM " + table + " WHERE model_version = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (modelVersion != null) {
                statement.setString(1, modelVersion);
            }
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding[i]);
        }
        return builder.append(']').toString();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private EmbeddingModelVersion toModelVersion(ResultSet rs) throws Exception {
        Timestamp activatedAt = rs.getTimestamp("activated_at");
        return new EmbeddingModelVersion(
                rs.getString("version_id"),
                rs.getString("model_name"),
                rs.getInt("dimensions"),
                rs.getString("status"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(),
                activatedAt == null ? null : activatedAt.toInstant(),
                rs.getString("notes")
        );
    }
}
