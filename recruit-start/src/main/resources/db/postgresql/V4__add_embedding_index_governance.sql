CREATE TABLE IF NOT EXISTS embedding_model_version (
    version_id VARCHAR(100) PRIMARY KEY,
    model_name VARCHAR(255) NOT NULL,
    dimensions INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP,
    notes TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_embedding_model_version_active
    ON embedding_model_version (active)
    WHERE active = TRUE;

CREATE TABLE IF NOT EXISTS embedding_index_rebuild_job (
    job_id VARCHAR(64) PRIMARY KEY,
    target_version VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_chunks INTEGER NOT NULL DEFAULT 0,
    succeeded_chunks INTEGER NOT NULL DEFAULT 0,
    failed_chunks INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    error_message TEXT
);

CREATE TABLE IF NOT EXISTS knowledge_chunk_embedding_version (
    model_version VARCHAR(100) NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_id BIGINT NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(1536) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (model_version, chunk_id)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_version_model
    ON knowledge_chunk_embedding_version (model_version);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_embedding_version_vector
    ON knowledge_chunk_embedding_version USING hnsw (embedding vector_cosine_ops);

INSERT INTO embedding_model_version(version_id, model_name, dimensions, status, active, activated_at, notes)
VALUES ('legacy-default', 'legacy-default', 1536, 'ACTIVE', TRUE, CURRENT_TIMESTAMP,
        'Auto-created for vectors written before model version governance.')
ON CONFLICT (version_id) DO NOTHING;

INSERT INTO knowledge_chunk_embedding_version(model_version, document_id, chunk_id, source_name, content, embedding)
SELECT 'legacy-default', document_id, chunk_id, source_name, content, embedding
FROM knowledge_chunk_embedding
ON CONFLICT (model_version, chunk_id) DO NOTHING;
