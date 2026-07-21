CREATE TABLE IF NOT EXISTS agent_trace (
    id BIGINT PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL,
    conversation_id BIGINT,
    node VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    details_json LONGTEXT,
    occurred_at TIMESTAMP(6) NOT NULL,
    INDEX idx_agent_trace_trace_id (trace_id),
    INDEX idx_agent_trace_conversation_id (conversation_id),
    INDEX idx_agent_trace_occurred_at (occurred_at)
);
