CREATE TABLE device_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID         NOT NULL,
    token       VARCHAR(512) NOT NULL,
    platform    VARCHAR(10)  NOT NULL CHECK (platform IN ('APNS', 'FCM')),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_token UNIQUE (user_id, token)
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);

CREATE TABLE notification_preferences (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              UUID    NOT NULL UNIQUE,
    prediction_reminder  BOOLEAN NOT NULL DEFAULT TRUE,
    race_start           BOOLEAN NOT NULL DEFAULT TRUE,
    results_published    BOOLEAN NOT NULL DEFAULT TRUE,
    score_amended        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW()
);
