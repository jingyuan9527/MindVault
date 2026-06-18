CREATE TABLE token_usage (
    id              BIGSERIAL PRIMARY KEY,
    model_id        BIGINT REFERENCES model_config(id) ON DELETE SET NULL,
    provider        VARCHAR(30) NOT NULL,
    model_name      VARCHAR(100) NOT NULL,
    model_type      VARCHAR(20) NOT NULL DEFAULT 'CHAT',
    prompt_tokens   INTEGER NOT NULL DEFAULT 0,
    completion_tokens INTEGER NOT NULL DEFAULT 0,
    total_tokens    INTEGER NOT NULL DEFAULT 0,
    cost            NUMERIC(10,6) NOT NULL DEFAULT 0,
    request_source  VARCHAR(30) NOT NULL DEFAULT 'CHAT',
    request_id      VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_token_usage_created ON token_usage(created_at DESC);
CREATE INDEX idx_token_usage_model ON token_usage(model_id);
CREATE INDEX idx_token_usage_date ON token_usage((created_at::date));

-- 按日汇总视图
CREATE MATERIALIZED VIEW token_usage_daily AS
SELECT
    created_at::date AS usage_date,
    provider,
    model_name,
    model_type,
    SUM(prompt_tokens) AS total_prompt_tokens,
    SUM(completion_tokens) AS total_completion_tokens,
    SUM(total_tokens) AS total_tokens,
    SUM(cost) AS total_cost,
    COUNT(*) AS request_count
FROM token_usage
GROUP BY created_at::date, provider, model_name, model_type
ORDER BY created_at::date DESC;