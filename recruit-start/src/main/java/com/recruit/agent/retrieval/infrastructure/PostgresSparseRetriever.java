package com.recruit.agent.retrieval.infrastructure;

import com.recruit.agent.retrieval.domain.RetrievalHit;
import com.recruit.agent.retrieval.domain.RetrievalRequest;
import com.recruit.agent.retrieval.domain.SparseRetriever;
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
public class PostgresSparseRetriever extends PostgresRetrieverSupport implements SparseRetriever {

    public PostgresSparseRetriever(VectorProperties properties) {
        super(properties);
    }

    @Override
    public List<RetrievalHit> search(String query, RetrievalRequest request) {
        StringBuilder sql = new StringBuilder("""
                WITH search_query AS (
                    SELECT plainto_tsquery('simple', ?) AS value
                )
                SELECT chunk.document_id, chunk.chunk_id, chunk.source_name, chunk.content,
                       ts_rank_cd(chunk.search_vector, search_query.value, 32) AS lexical_score
                FROM knowledge_chunk_embedding chunk
                CROSS JOIN search_query
                WHERE chunk.search_vector @@ search_query.value
                """);
        appendMetadataFilters(sql, request.filters(), "chunk.");
        sql.append("""
                ORDER BY lexical_score DESC, chunk.chunk_id ASC
                LIMIT ?
                """);

        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setString(index++, query);
            index = bindMetadataFilters(connection, statement, index, request.filters());
            statement.setInt(index, request.candidateLimit());
            return readResults(statement);
        } catch (Exception exception) {
            throw new IllegalStateException("PostgreSQL sparse retrieval failed", exception);
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
                        resultSet.getDouble("lexical_score")
                ));
            }
        }
        return List.copyOf(results);
    }
}
