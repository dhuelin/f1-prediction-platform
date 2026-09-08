CREATE TABLE prediction_participation_stats (
    id           BIGSERIAL    PRIMARY KEY,
    race_id      VARCHAR(64)  NOT NULL UNIQUE,
    locked_count INT          NOT NULL,
    session_type VARCHAR(32),
    locked_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_participation_locked_at ON prediction_participation_stats (locked_at DESC);
