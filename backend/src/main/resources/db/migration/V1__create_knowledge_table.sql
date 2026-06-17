-- V1__create_knowledge_table.sql
-- MindVault 核心表：知识条目
--
-- 设计思路：
-- - embedding 字段存储向量嵌入（1536 维 = OpenAI/通义千问默认维度）
-- - HNSW 索引提供近似最近邻搜索，比 IVFFlat 精度更高
-- - metadata 为 JSONB 类型，v0.2+ 扩展字段无需改表
-- - content_type 预埋 TEXT|PDF|URL|IMAGE 枚举

-- 启用 pgvector 扩展（如果尚未启用）
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE knowledge (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    content         TEXT NOT NULL,
    content_type    VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    source_url      TEXT,
    summary         TEXT,
    tags            JSONB DEFAULT '[]'::jsonb,
    embedding       VECTOR(1536),
    metadata        JSONB DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- HNSW 索引：适合高维向量近似最近邻搜索
-- m = 16: 每个节点的最大连接数（越大精度越高，但索引更大）
-- ef_construction = 200: 构建索引时的动态列表大小
CREATE INDEX idx_knowledge_embedding ON knowledge
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 200);

-- GIN 全文检索索引（v0.2 混合检索时使用）
CREATE INDEX idx_knowledge_content_fts ON knowledge
    USING gin (to_tsvector('simple', coalesce(content, '') || ' ' || coalesce(title, '')));

-- 更新时间自动更新触发器
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_knowledge_updated_at
    BEFORE UPDATE ON knowledge
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();