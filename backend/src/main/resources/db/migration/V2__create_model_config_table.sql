-- V2__create_model_config_table.sql
-- 模型配置表
--
-- 设计思路：
-- - model_type 区分 CHAT(对话) / EMBEDDING(嵌入) / SUMMARIZE(摘要)
-- - v0.1 全局只有一个主模型；v0.3 改为每种 model_type 各有一个主模型
-- - is_primary 的唯一索引使用了 PostgreSQL 部分索引特性
-- - metadata JSONB 预存 temperature、max_tokens 等模型参数

CREATE TABLE model_config (
    id              BIGSERIAL PRIMARY KEY,
    provider        VARCHAR(30) NOT NULL,
    model_name      VARCHAR(100) NOT NULL,
    model_type      VARCHAR(20) NOT NULL DEFAULT 'CHAT',
    api_key         TEXT,
    base_url        VARCHAR(500),
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    is_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    priority        INT NOT NULL DEFAULT 0,
    metadata        JSONB DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- v0.1: 全局只有一个主模型
-- v0.3 改为: 每种 model_type 各有一个主模型
CREATE UNIQUE INDEX idx_single_primary
    ON model_config (is_primary)
    WHERE is_primary = TRUE;

CREATE TRIGGER trg_model_config_updated_at
    BEFORE UPDATE ON model_config
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();