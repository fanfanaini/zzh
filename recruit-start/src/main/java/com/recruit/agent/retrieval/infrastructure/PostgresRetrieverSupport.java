package com.recruit.agent.retrieval.infrastructure;

import com.recruit.agent.retrieval.domain.RetrievalFilters;
import com.recruit.agent.vector.infrastructure.VectorProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

abstract class PostgresRetrieverSupport {

    private final VectorProperties properties;

    PostgresRetrieverSupport(VectorProperties properties) {
        this.properties = properties;
    }

    protected Connection connection() throws Exception {
        if (properties.getDriverClassName() != null && !properties.getDriverClassName().isBlank()) {
            Class.forName(properties.getDriverClassName());
        }
        return DriverManager.getConnection(properties.getUrl(), properties.getUsername(), properties.getPassword());
    }

    protected void appendMetadataFilters(StringBuilder sql, RetrievalFilters filters, String prefix) {
        if (filters.hasDocumentIds()) {
            sql.append(" AND ").append(prefix).append("document_id = ANY (?)");
        }
        if (filters.hasSourceNames()) {
            sql.append(" AND ").append(prefix).append("source_name = ANY (?)");
        }
    }

    protected int bindMetadataFilters(Connection connection, PreparedStatement statement, int index,
                                      RetrievalFilters filters) throws SQLException {
        if (filters.hasDocumentIds()) {
            statement.setArray(index++, connection.createArrayOf("bigint", filters.documentIds().toArray()));
        }
        if (filters.hasSourceNames()) {
            statement.setArray(index++, connection.createArrayOf("text", filters.sourceNames().toArray()));
        }
        return index;
    }

    protected String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < embedding.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(embedding[index]);
        }
        return builder.append(']').toString();
    }
}
