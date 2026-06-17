-- V3__create_chat_tables.sql
-- 对话记录表
--
-- 设计思路：
-- - session/message 一对多，级联删除
-- - metadata 存储 Token 用量、模型信息等非结构化数据

CREATE TABLE chat_session (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE chat_message (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL REFERENCES chat_session(id) ON DELETE CASCADE,
    role            VARCHAR(10) NOT NULL,
    content         TEXT NOT NULL,
    metadata        JSONB DEFAULT '{}'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_message_session ON chat_message(session_id, created_at);

CREATE TRIGGER trg_chat_session_updated_at
    BEFORE UPDATE ON chat_session
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();