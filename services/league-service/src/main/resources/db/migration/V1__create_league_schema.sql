CREATE TABLE leagues (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL UNIQUE,
    visibility    VARCHAR(10) NOT NULL DEFAULT 'PUBLIC',
    invite_code   VARCHAR(10),
    admin_user_id UUID NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_visibility CHECK (visibility IN ('PUBLIC','PRIVATE')),
    CONSTRAINT chk_invite_code CHECK (
        (visibility = 'PRIVATE' AND invite_code IS NOT NULL) OR
        (visibility = 'PUBLIC'  AND invite_code IS NULL)
    )
);

CREATE TABLE league_members (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id       UUID NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    joined_at       TIMESTAMP NOT NULL DEFAULT now(),
    catch_up_points INT NOT NULL DEFAULT 0,
    UNIQUE (league_id, user_id)
);

CREATE TABLE scoring_configs (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id              UUID NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    effective_from_race    INT NOT NULL DEFAULT 1,
    prediction_depth       INT NOT NULL DEFAULT 10,
    exact_position_points  INT NOT NULL DEFAULT 10,
    offset_tiers           TEXT NOT NULL DEFAULT '[{"offset":1,"points":7},{"offset":2,"points":2}]',
    in_range_points        INT NOT NULL DEFAULT 1,
    bet_multiplier         NUMERIC(3,1) NOT NULL DEFAULT 2.0,
    active_bets            TEXT NOT NULL DEFAULT '{"fastestLap":true,"dnfDsqDns":true,"scDeployed":true,"scCount":true}',
    sprint_scoring_enabled BOOLEAN NOT NULL DEFAULT true,
    max_stake_per_bet      INT,
    UNIQUE (league_id, effective_from_race)
);

CREATE INDEX idx_league_members_league ON league_members(league_id);
CREATE INDEX idx_league_members_user   ON league_members(user_id);
CREATE INDEX idx_scoring_configs_league ON scoring_configs(league_id);
