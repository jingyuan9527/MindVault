-- V4__create_operation_log_table.sql
-- 操作日志表
--
-- 用途：记录所有关键操作，审计追溯
-- module: KNOWLEDGE / CHAT / MODEL / SYSTEM
-- action: ADD / SEARCH / DELETE / EXPORT / TEST / ERROR
-- detail: JSONB 存储请求参数、耗时等扩展信息

CREATE TABLE operation_log (
    id              BIGSERIAL PRIMARY KEY,
    module          VARCHAR(20) NOT NULL,
    action          VARCHAR(30) NOT NULL,
    entity_id       BIGINT,
    summary         VARCHAR(500),
    detail          JSONB,
    operator        VARCHAR(100) DEFAULT 'system',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_oplog_module ON operation_log(module, created_at);