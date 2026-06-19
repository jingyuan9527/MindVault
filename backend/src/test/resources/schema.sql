CREATE TABLE IF NOT EXISTS model_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider        VARCHAR(30) NOT NULL,
    model_name      VARCHAR(100) NOT NULL,
    model_type      VARCHAR(20) NOT NULL DEFAULT 'CHAT',
    api_key         TEXT,
    base_url        VARCHAR(500),
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    priority        INT NOT NULL DEFAULT 0,
    metadata        TEXT DEFAULT '{}',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    title               VARCHAR(500),
    ai_title            VARCHAR(500),
    content             TEXT NOT NULL,
    content_type        VARCHAR(20) DEFAULT 'TEXT',
    source_url          VARCHAR(500),
    summary             TEXT,
    tags                TEXT DEFAULT '[]',
    user_tags           TEXT DEFAULT '[]',
    embedding           TEXT,
    metadata            TEXT DEFAULT '{}',
    auto_process_status VARCHAR(20) DEFAULT 'PENDING',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_relation (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    knowledge_id    BIGINT NOT NULL,
    related_id      BIGINT NOT NULL,
    relation_type   VARCHAR(20) NOT NULL,
    score           DECIMAL(3,2) DEFAULT 0,
    source          VARCHAR(20) DEFAULT 'VECTOR',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS auto_process_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    knowledge_id    BIGINT NOT NULL,
    round           VARCHAR(20) NOT NULL,
    status          VARCHAR(10) NOT NULL,
    result_summary  TEXT,
    llm_tokens      INT DEFAULT 0,
    llm_duration_ms INT DEFAULT 0,
    error_message   TEXT,
    started_at      TIMESTAMP NOT NULL,
    completed_at    TIMESTAMP NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
