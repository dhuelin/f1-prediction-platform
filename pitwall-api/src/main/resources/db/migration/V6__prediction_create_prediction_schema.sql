CREATE TABLE predictions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    race_id     VARCHAR(50) NOT NULL,
    session_type VARCHAR(20) NOT NULL,
    locked      BOOLEAN NOT NULL DEFAULT false,
    submitted_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, race_id, session_type)
);

CREATE TABLE prediction_entries (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prediction_id UUID NOT NULL REFERENCES predictions(id) ON DELETE CASCADE,
    position      INT NOT NULL CHECK (position >= 1),
    driver_code   VARCHAR(10) NOT NULL
);

CREATE TABLE bonus_bets (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prediction_id UUID NOT NULL REFERENCES predictions(id) ON DELETE CASCADE,
    bet_type      VARCHAR(30) NOT NULL,
    stake         INT NOT NULL CHECK (stake > 0),
    bet_value     TEXT NOT NULL,
    settled       BOOLEAN NOT NULL DEFAULT false,
    won           BOOLEAN,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_predictions_user_race ON predictions(user_id, race_id);
CREATE INDEX idx_predictions_race ON predictions(race_id);
CREATE INDEX idx_bonus_bets_prediction ON bonus_bets(prediction_id);
