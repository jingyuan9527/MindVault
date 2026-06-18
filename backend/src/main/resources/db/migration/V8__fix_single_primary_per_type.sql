-- V8__fix_single_primary_per_type.sql
-- 修复主模型唯一约束：每种 model_type 各有一个主模型
-- Instead of global unique primary, enforce per-model-type unique primary

DROP INDEX IF EXISTS idx_single_primary;

CREATE UNIQUE INDEX idx_single_primary_per_type
    ON model_config (model_type)
    WHERE is_primary = TRUE;