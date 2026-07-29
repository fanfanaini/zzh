CREATE TABLE IF NOT EXISTS job_application (
    id BIGINT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    stage VARCHAR(32) NOT NULL,
    owner_user_id BIGINT,
    source VARCHAR(64),
    latest_evaluation_id BIGINT,
    latest_conversation_id BIGINT,
    rejection_reason VARCHAR(512),
    note VARCHAR(1024),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL,
    UNIQUE KEY uk_job_application_job_candidate (job_id, candidate_id),
    INDEX idx_job_application_job_stage (job_id, stage),
    INDEX idx_job_application_candidate (candidate_id),
    INDEX idx_job_application_updated_at (updated_at)
);

CREATE TABLE IF NOT EXISTS job_application_history (
    id BIGINT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    from_stage VARCHAR(32),
    to_stage VARCHAR(32) NOT NULL,
    operator_user_id BIGINT,
    reason VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_job_application_history_application (application_id, created_at)
);
