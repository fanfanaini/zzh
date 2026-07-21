CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(64) NOT NULL,
    http_method VARCHAR(16) NOT NULL,
    request_path VARCHAR(512) NOT NULL,
    status_code INT NOT NULL,
    duration_millis BIGINT NOT NULL,
    client_ip VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_log_user_id (user_id),
    INDEX idx_audit_log_created_at (created_at)
);
