-- users
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    github_id  VARCHAR(255) NOT NULL UNIQUE,
    email      VARCHAR(255) NOT NULL,
    nickname   VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(255),
    html_url   VARCHAR(255),
    role       VARCHAR(50)  NOT NULL
);

-- restaurants
CREATE TABLE restaurants (
    id           BIGSERIAL PRIMARY KEY,
    kakao_api_id VARCHAR(255) NOT NULL UNIQUE,
    name         VARCHAR(255) NOT NULL,
    category     VARCHAR(255) NOT NULL,
    place_url    VARCHAR(500) NOT NULL,
    address      VARCHAR(500) NOT NULL,
    x            DOUBLE PRECISION NOT NULL,
    y            DOUBLE PRECISION NOT NULL
);

CREATE INDEX idx_restaurant_x_y ON restaurants (x, y);

-- reviews
CREATE TABLE reviews (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    restaurant_id BIGINT       NOT NULL REFERENCES restaurants (id),
    rating        INTEGER      NOT NULL CHECK (rating BETWEEN 0 AND 5),
    comment       VARCHAR(100) NOT NULL,
    visit_number  INTEGER      NOT NULL,
    status        VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP
);

CREATE INDEX idx_review_user_latest       ON reviews (user_id, created_at);
CREATE INDEX idx_review_restaurant_latest ON reviews (restaurant_id, created_at);

-- partial unique: 한 유저가 한 식당에 같은 visitNumber로 ACTIVE 리뷰는 최대 1개
CREATE UNIQUE INDEX uq_review_user_restaurant_visit_active
    ON reviews (user_id, restaurant_id, visit_number)
    WHERE status = 'ACTIVE';

-- restaurant_review_summary
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
