CREATE TABLE IF NOT EXISTS conversation_summary (
    conversation_id BIGINT PRIMARY KEY,
    content TEXT NOT NULL,
    summarized_message_count INT NOT NULL,
    summarized_through_message_id BIGINT,
    token_count INT NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    INDEX idx_conversation_summary_updated_at (updated_at)
);
