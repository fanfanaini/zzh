package com.recruit.agent.retrieval.infrastructure;

import com.recruit.agent.retrieval.domain.DenseRetriever;
import com.recruit.agent.retrieval.domain.RetrievalHit;
import com.recruit.agent.retrieval.domain.RetrievalRequest;
import com.recruit.agent.vector.infrastructure.VectorProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Repository
@Profile("vector")
public class PostgresDenseRetriever extends PostgresRetrieverSupport implements DenseRetriever {

    public PostgresDenseRetriever(VectorProperties properties) {
        super(properties);
    }

    @Override
    public List<RetrievalHit> search(float[] queryEmbedding, RetrievalRequest request) {
        StringBuilder sql = new StringBuilder("""
                SELECT document_id, chunk_id, source_name, content, similarity
                FROM (
                    SELECT document_id, chunk_id, source_name, content,
                           1 - (embedding <=> ?::vector) AS similarity
                    FROM knowledge_chunk_embedding
                    WHERE 1 = 1
                """);
        appendMetadataFilters(sql, request.filters(), "");
        sql.append("""
                ) ranked
                WHERE similarity >= ?
                ORDER BY similarity DESC, chunk_id ASC
                LIMIT ?
                """);

        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setString(index++, toVectorLiteral(queryEmbedding));
            index = bindMetadataFilters(connection, statement, index, request.filters());
            statement.setDouble(index++, request.minDenseScore() / 100.0);
            statement.setInt(index, request.candidateLimit());
            return readResults(statement);
        } catch (Exception exception) {
            throw new IllegalStateException("PostgreSQL dense retrieval failed", exception);
        }
    }

    private List<RetrievalHit> readResults(PreparedStatement statement) throws Exception {
        List<RetrievalHit> results = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                results.add(new RetrievalHit(
                        resultSet.getLong("document_id"),
                        resultSet.getLong("chunk_id"),
                        resultSet.getString("source_name"),
                        resultSet.getString("content"),
                        round(resultSet.getDouble("similarity") * 100)
                ));
            }
        }
        return List.copyOf(results);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
