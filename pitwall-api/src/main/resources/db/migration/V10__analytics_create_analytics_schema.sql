CREATE TABLE race_analytics_events (
    id           BIGSERIAL    PRIMARY KEY,
    event_type   VARCHAR(64)  NOT NULL,
    race_id      VARCHAR(64)  NOT NULL,
    league_id    VARCHAR(64),
    payload      TEXT,
    occurred_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analytics_events_race_id    ON race_analytics_events (race_id);
CREATE INDEX idx_analytics_events_event_type ON race_analytics_events (event_type);
CREATE INDEX idx_analytics_events_occurred   ON race_analytics_events (occurred_at DESC);
