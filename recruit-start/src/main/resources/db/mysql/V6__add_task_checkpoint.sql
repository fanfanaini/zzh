CREATE TABLE IF NOT EXISTS task_checkpoint (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    checkpoint_key VARCHAR(128) NOT NULL,
    completed_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_task_checkpoint (task_id, checkpoint_key),
    KEY idx_task_checkpoint_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
