CREATE TABLE IF NOT EXISTS knowledge_chunk_embedding (
    document_id BIGINT NOT NULL,
    chunk_id BIGINT PRIMARY KEY,
    source_name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(1536) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_vector
    ON knowledge_chunk_embedding USING hnsw (embedding vector_cosine_ops);
