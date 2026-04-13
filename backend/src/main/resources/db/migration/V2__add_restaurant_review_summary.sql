CREATE TABLE restaurant_review_summary (
    restaurant_id       BIGINT PRIMARY KEY REFERENCES restaurants (id),
    summary_json        JSONB        NOT NULL,
    source_hash         VARCHAR(64)  NOT NULL,
    source_review_count INTEGER      NOT NULL,
    source_review_ids   BIGINT[]     NOT NULL,
    model_version       VARCHAR(50)  NOT NULL,
    prompt_version      VARCHAR(20)  NOT NULL,
    validation_status   VARCHAR(20)  NOT NULL DEFAULT 'pending',
    generated_at        TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_summary_generated_at ON restaurant_review_summary (generated_at);
