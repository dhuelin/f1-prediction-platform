CREATE TABLE race_scores (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    league_id           UUID NOT NULL,
    race_id             VARCHAR(50) NOT NULL,
    session_type        VARCHAR(20) NOT NULL,
    top_n_points        INT NOT NULL DEFAULT 0,
    bonus_points        INT NOT NULL DEFAULT 0,
    total_points        INT NOT NULL DEFAULT 0,
    is_partial_distance BOOLEAN NOT NULL DEFAULT false,
    is_cancelled        BOOLEAN NOT NULL DEFAULT false,
    scored_at           TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, league_id, race_id, session_type)
);

CREATE TABLE league_standings (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,
    league_id    UUID NOT NULL,
    total_points INT NOT NULL DEFAULT 0,
    rank         INT NOT NULL DEFAULT 0,
    updated_at   TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, league_id)
);

CREATE INDEX idx_race_scores_league_race ON race_scores(league_id, race_id);
CREATE INDEX idx_race_scores_user_league ON race_scores(user_id, league_id);
CREATE INDEX idx_standings_league        ON league_standings(league_id);
