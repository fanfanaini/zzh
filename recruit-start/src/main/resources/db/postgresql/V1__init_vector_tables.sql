CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS resume_project_embedding (
    id BIGSERIAL PRIMARY KEY,
    candidate_id BIGINT NOT NULL,
    project_id BIGINT,
    content TEXT NOT NULL,
    embedding vector(1536),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_description_embedding (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_description_embedding_job_id
    ON job_description_embedding (job_id);

CREATE INDEX IF NOT EXISTS idx_resume_project_embedding_candidate_id
    ON resume_project_embedding (candidate_id);

CREATE INDEX IF NOT EXISTS idx_job_description_embedding_vector
    ON job_description_embedding
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

CREATE INDEX IF NOT EXISTS idx_resume_project_embedding_vector
    ON resume_project_embedding
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
