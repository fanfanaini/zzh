CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    roles JSON NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_app_user_username (username)
);

CREATE TABLE IF NOT EXISTS job (
    id BIGINT PRIMARY KEY,
    job_name VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    required_skills JSON NOT NULL,
    preferred_skills JSON NOT NULL,
    education_requirement VARCHAR(64),
    min_experience_years DECIMAL(4,1),
    max_experience_years DECIMAL(4,1),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS candidate (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    phone_hash VARCHAR(128),
    email_hash VARCHAR(128),
    education_level VARCHAR(32) NOT NULL,
    total_experience_years DECIMAL(4,1) NOT NULL,
    expected_city VARCHAR(64),
    skills JSON NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resume (
    id BIGINT PRIMARY KEY,
    candidate_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(128),
    file_size BIGINT NOT NULL,
    storage_object_name VARCHAR(512) NOT NULL,
    parse_status VARCHAR(32) NOT NULL,
    raw_text LONGTEXT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resume_candidate_id (candidate_id)
);

CREATE TABLE IF NOT EXISTS resume_evaluation (
    id BIGINT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    rule_score DECIMAL(5,2),
    semantic_score DECIMAL(5,2),
    final_score DECIMAL(5,2),
    recommendation VARCHAR(32),
    confidence DECIMAL(5,4),
    advantages JSON,
    risks JSON,
    missing_information JSON,
    model_name VARCHAR(64),
    prompt_version VARCHAR(32),
    human_result VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    stage VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS conversation_message (
    id BIGINT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_message_conversation_id (conversation_id)
);

CREATE TABLE IF NOT EXISTS reply_suggestion (
    id BIGINT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    intent VARCHAR(32) NOT NULL,
    confidence DECIMAL(5,4),
    need_human BOOLEAN NOT NULL,
    suggested_reply TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reply_suggestion_conversation_id (conversation_id)
);

CREATE TABLE IF NOT EXISTS agent_task (
    id BIGINT PRIMARY KEY,
    task_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    target_id BIGINT,
    total_count INT NOT NULL,
    success_count INT NOT NULL,
    failure_count INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_step (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    step_name VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_step_task_id (task_id)
);

CREATE TABLE IF NOT EXISTS prompt_template (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_prompt_template_code (code)
);

CREATE TABLE IF NOT EXISTS prompt_version (
    id BIGINT PRIMARY KEY,
    template_code VARCHAR(64) NOT NULL,
    version VARCHAR(32) NOT NULL,
    system_prompt TEXT NOT NULL,
    user_prompt_template TEXT NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_prompt_version_code_version (template_code, version)
);

CREATE TABLE IF NOT EXISTS model_call_log (
    id BIGINT PRIMARY KEY,
    use_case VARCHAR(64) NOT NULL,
    model_name VARCHAR(64) NOT NULL,
    prompt_version VARCHAR(32),
    system_prompt LONGTEXT,
    user_prompt LONGTEXT,
    response_body LONGTEXT,
    prompt_tokens INT,
    completion_tokens INT,
    latency_millis BIGINT,
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_model_call_log_use_case (use_case),
    INDEX idx_model_call_log_created_at (created_at)
);
