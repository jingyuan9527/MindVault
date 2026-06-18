CREATE TABLE flash_card (
    id BIGSERIAL PRIMARY KEY,
    knowledge_id BIGINT NOT NULL REFERENCES knowledge(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    difficulty VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    source_type VARCHAR(10) NOT NULL DEFAULT 'AUTO',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_flash_card_knowledge_id ON flash_card(knowledge_id);