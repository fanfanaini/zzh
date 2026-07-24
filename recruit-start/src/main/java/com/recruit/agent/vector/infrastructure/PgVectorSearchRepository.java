package com.recruit.agent.vector.infrastructure;

import com.recruit.agent.vector.domain.ProjectSimilarity;
import com.recruit.agent.vector.domain.VectorSearchRepository;
import com.recruit.agent.vector.domain.KnowledgeChunkMatch;
import com.recruit.agent.vector.domain.KnowledgeDocumentSummary;
import com.recruit.agent.vector.domain.KnowledgeChunk;
import com.recruit.agent.vector.domain.KnowledgeChunkWrite;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

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
            throw new IllegalStateException("pgvector 查询失败", exception);
        }
    }

    @Override
    public void saveKnowledgeChunk(Long documentId, Long chunkId, String sourceName, String content, float[] embedding) {
        execute("""
                INSERT INTO knowledge_chunk_embedding(document_id, chunk_id, source_name, content, embedding)
                VALUES (?, ?, ?, ?, ?::vector)
                ON CONFLICT (chunk_id) DO UPDATE SET source_name = EXCLUDED.source_name,
                    content = EXCLUDED.content, embedding = EXCLUDED.embedding
                """, documentId, chunkId, sourceName, content, toVectorLiteral(embedding));
    }

    @Override
    public void saveKnowledgeDocument(List<KnowledgeChunkWrite> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("Knowledge document chunks must not be empty");
        }
        String sql = """
                INSERT INTO knowledge_chunk_embedding(document_id, chunk_id, source_name, content, embedding)
                VALUES (?, ?, ?, ?, ?::vector)
                ON CONFLICT (chunk_id) DO UPDATE SET source_name = EXCLUDED.source_name,
                    content = EXCLUDED.content, embedding = EXCLUDED.embedding
                """;
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            try {
                for (KnowledgeChunkWrite chunk : chunks) {
                    statement.setLong(1, chunk.documentId());
                    statement.setLong(2, chunk.chunkId());
                    statement.setString(3, chunk.sourceName());
                    statement.setString(4, chunk.content());
                    statement.setString(5, toVectorLiteral(chunk.embedding()));
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector 知识文档事务写入失败", exception);
        }
    }

    @Override
    public List<KnowledgeChunkMatch> searchKnowledge(float[] queryEmbedding, int limit) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT document_id, chunk_id, source_name, content, 1 - (embedding <=> ?::vector) AS similarity
                     FROM knowledge_chunk_embedding ORDER BY embedding <=> ?::vector LIMIT ?
                     """)) {
            String vector = toVectorLiteral(queryEmbedding);
            statement.setString(1, vector);
            statement.setString(2, vector);
            statement.setInt(3, limit);
            List<KnowledgeChunkMatch> matches = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    matches.add(new KnowledgeChunkMatch(rs.getLong("document_id"), rs.getLong("chunk_id"),
                            rs.getString("source_name"), rs.getString("content"), round(rs.getDouble("similarity") * 100)));
                }
            }
            return List.copyOf(matches);
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector 知识库检索失败", exception);
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
            throw new IllegalStateException("pgvector 知识文档查询失败", exception);
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
            throw new IllegalStateException("pgvector 知识切片查询失败", exception);
        }
    }

    @Override
    public boolean deleteKnowledgeDocument(Long documentId) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM knowledge_chunk_embedding WHERE document_id = ?")) {
            statement.setLong(1, documentId);
            return statement.executeUpdate() > 0;
        } catch (Exception exception) {
            throw new IllegalStateException("pgvector 知识文档删除失败", exception);
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
            throw new IllegalStateException("pgvector 写入失败", exception);
        }
    }

    private Connection connection() throws Exception {
        if (properties.getDriverClassName() != null && !properties.getDriverClassName().isBlank()) {
            Class.forName(properties.getDriverClassName());
        }
        return DriverManager.getConnection(properties.getUrl(), properties.getUsername(), properties.getPassword());
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
}
