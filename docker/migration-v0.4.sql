-- v0.4 Migration: AI auto-processing pipeline
-- Run: psql -U postgres -d mindvault -f migration-v0.4.sql

-- 1. Add new columns to knowledge table
ALTER TABLE knowledge ADD COLUMN IF NOT EXISTS ai_title VARCHAR(500);
ALTER TABLE knowledge ADD COLUMN IF NOT EXISTS user_tags JSONB DEFAULT '[]';
ALTER TABLE knowledge ADD COLUMN IF NOT EXISTS auto_process_status VARCHAR(20) DEFAULT 'PENDING';

-- 2. Create knowledge_relation table
CREATE TABLE IF NOT EXISTS knowledge_relation (
    id              BIGSERIAL PRIMARY KEY,
    knowledge_id    BIGINT NOT NULL REFERENCES knowledge(id) ON DELETE CASCADE,
    related_id      BIGINT NOT NULL REFERENCES knowledge(id) ON DELETE CASCADE,
    relation_type   VARCHAR(20) NOT NULL,
    score           DECIMAL(3,2) DEFAULT 0,
    source          VARCHAR(20) DEFAULT 'VECTOR',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(knowledge_id, related_id, source)
);

-- 3. Create auto_process_log table
CREATE TABLE IF NOT EXISTS auto_process_log (
    id              BIGSERIAL PRIMARY KEY,
    knowledge_id    BIGINT NOT NULL REFERENCES knowledge(id) ON DELETE CASCADE,
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

-- 4. Backfill: migrate existing title -> ai_title (as legacy AI titles)
UPDATE knowledge SET ai_title = title WHERE ai_title IS NULL AND title IS NOT NULL;

-- 5. Backfill: set existing tags as user_tags (for existing notes, treat existing tags as user tags)
UPDATE knowledge SET user_tags = tags WHERE user_tags = '[]' AND tags IS NOT NULL AND tags != '[]';
