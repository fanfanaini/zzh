CREATE TABLE IF NOT EXISTS candidate_memory (
    id BIGINT PRIMARY KEY,
    candidate_id BIGINT NOT NULL,
    memory_type VARCHAR(32) NOT NULL,
    fact_key VARCHAR(128) NOT NULL,
    fact_value TEXT NOT NULL,
    confidence DECIMAL(5,4) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_id BIGINT NULL,
    evidence TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_candidate_memory_key (candidate_id, fact_key, active),
    KEY idx_candidate_memory_candidate_active (candidate_id, active, updated_at),
    KEY idx_candidate_memory_type (memory_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
