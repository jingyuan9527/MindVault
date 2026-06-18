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