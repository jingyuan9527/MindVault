-- ============================================================
-- Migration v0.6 — 操作日志重构：分区表 + 归档表
-- ============================================================

-- 1. 操作日志主表（按月分区）
DROP TABLE IF EXISTS operation_log CASCADE;
CREATE TABLE operation_log (
    id              BIGSERIAL,
    module          VARCHAR(32)  NOT NULL,
    action          VARCHAR(32)  NOT NULL,
    action_type     VARCHAR(10)  NOT NULL DEFAULT 'OTHER',
    entity_id       VARCHAR(64),
    summary         VARCHAR(500) NOT NULL DEFAULT '',
    detail          JSONB,
    before_snapshot JSONB,
    after_snapshot  JSONB,
    operator        VARCHAR(64)  NOT NULL DEFAULT 'system',
    operator_id     BIGINT,
    ip_address      VARCHAR(45),
    result          VARCHAR(10)  NOT NULL DEFAULT 'SUCCESS',
    error_message   TEXT,
    duration_ms     INTEGER      NOT NULL DEFAULT 0,
    remark          VARCHAR(255),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 2. 归档表（与主表结构一致，可放在不同表空间）
DROP TABLE IF EXISTS operation_log_archive CASCADE;
CREATE TABLE operation_log_archive (
    LIKE operation_log INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING STORAGE
) PARTITION BY RANGE (created_at);

-- 3. 索引（热表）
CREATE INDEX idx_operation_log_module ON operation_log (module, created_at DESC);
CREATE INDEX idx_operation_log_operator ON operation_log (operator_id, created_at DESC);
CREATE INDEX idx_operation_log_action ON operation_log (action_type, created_at DESC);

-- 4. 归档表索引
CREATE INDEX idx_operation_log_archive_created_at ON operation_log_archive (created_at DESC);
CREATE INDEX idx_operation_log_archive_module ON operation_log_archive (module, created_at DESC);

-- 5. 创建初始分区（当前月 + 未来 2 个月）
DO $$
DECLARE
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
    archive_name TEXT;
BEGIN
    FOR i IN 0..2 LOOP
        start_date := DATE_TRUNC('month', CURRENT_DATE) + (i || ' months')::INTERVAL;
        end_date := start_date + INTERVAL '1 month';
        partition_name := 'operation_log_' || TO_CHAR(start_date, 'YYYY_MM');

        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = partition_name) THEN
            EXECUTE format(
                'CREATE TABLE %I PARTITION OF operation_log FOR VALUES FROM (%L) TO (%L)',
                partition_name, start_date, end_date
            );
        END IF;
    END LOOP;
END $$;

-- 6. 分区自动创建函数（每月 1 号执行）
CREATE OR REPLACE FUNCTION create_next_month_partition()
RETURNS void AS $$
DECLARE
    next_month DATE;
    partition_name TEXT;
BEGIN
    next_month := DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '3 months';
    partition_name := 'operation_log_' || TO_CHAR(next_month, 'YYYY_MM');

    IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = partition_name) THEN
        EXECUTE format(
            'CREATE TABLE %I PARTITION OF operation_log FOR VALUES FROM (%L) TO (%L)',
            partition_name, next_month, next_month + INTERVAL '1 month'
        );
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 7. 分区归档函数（将 N 个月前的分区从热表移到归档表）
CREATE OR REPLACE FUNCTION archive_operation_log_partition(months_ago INTEGER)
RETURNS void AS $$
DECLARE
    target_month DATE;
    partition_name TEXT;
    archive_name TEXT;
BEGIN
    target_month := DATE_TRUNC('month', CURRENT_DATE) - (months_ago || ' months')::INTERVAL;
    partition_name := 'operation_log_' || TO_CHAR(target_month, 'YYYY_MM');
    archive_name := 'operation_log_archive_' || TO_CHAR(target_month, 'YYYY_MM');

    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = partition_name) THEN
        -- 从热表分离
        EXECUTE format('ALTER TABLE operation_log DETACH PARTITION %I', partition_name);

        -- 创建归档分区（如不存在）
        IF NOT EXISTS (SELECT 1 FROM pg_class WHERE relname = archive_name) THEN
            EXECUTE format(
                'CREATE TABLE %I (LIKE operation_log_archive INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING STORAGE)',
                archive_name
            );
            EXECUTE format(
                'ALTER TABLE operation_log_archive ATTACH PARTITION %I FOR VALUES FROM (%L) TO (%L)',
                archive_name, target_month, target_month + INTERVAL '1 month'
            );
        END IF;

        -- 数据搬入归档分区
        EXECUTE format('INSERT INTO %I SELECT * FROM ONLY %I', archive_name, partition_name);
        EXECUTE format('DROP TABLE %I', partition_name);
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 8. 系统配置
INSERT INTO system_config (config_key, config_value, description, value_type)
VALUES ('threshold.operationlog.retention-months', '3', '操作日志热数据保留月数', 'int')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (config_key, config_value, description, value_type)
VALUES ('threshold.operationlog.archive-retention-months', '12', '操作日志归档保留月数', 'int')
ON CONFLICT (config_key) DO NOTHING;