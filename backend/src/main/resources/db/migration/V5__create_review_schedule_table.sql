CREATE TABLE review_schedule (
    id              BIGSERIAL PRIMARY KEY,
    knowledge_id    BIGINT NOT NULL REFERENCES knowledge(id) ON DELETE CASCADE,
    ease_factor     NUMERIC(4,2) NOT NULL DEFAULT 2.50,
    interval_days   INTEGER NOT NULL DEFAULT 0,
    review_count    INTEGER NOT NULL DEFAULT 0,
    next_review_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    last_review_at  TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_review_schedule_next ON review_schedule(next_review_at);
CREATE UNIQUE INDEX idx_review_schedule_knowledge ON review_schedule(knowledge_id);