CREATE TABLE daily_review (
    id BIGSERIAL PRIMARY KEY,
    report_date DATE NOT NULL UNIQUE,
    total_count INTEGER NOT NULL DEFAULT 0,
    summary TEXT,
    key_insights TEXT NOT NULL DEFAULT '[]',
    recommendations TEXT NOT NULL DEFAULT '[]',
    category_breakdown TEXT NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_daily_review_report_date ON daily_review(report_date DESC);