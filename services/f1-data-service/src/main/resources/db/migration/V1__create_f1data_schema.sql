CREATE TABLE races (
    id VARCHAR(50) PRIMARY KEY,  -- e.g. "2025-01" (season-round)
    season INT NOT NULL,
    round INT NOT NULL,
    race_name VARCHAR(100) NOT NULL,
    circuit_name VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    race_date TIMESTAMPTZ,
    is_sprint_weekend BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(season, round)
);

CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    race_id VARCHAR(50) NOT NULL REFERENCES races(id),
    session_type VARCHAR(20) NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT false,
    completed_at TIMESTAMPTZ
);

CREATE TABLE drivers (
    code VARCHAR(3) PRIMARY KEY,
    driver_number INT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    nationality VARCHAR(50),
    season INT NOT NULL
);

CREATE TABLE race_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    race_id VARCHAR(50) NOT NULL REFERENCES races(id),
    session_type VARCHAR(20) NOT NULL,
    driver_code VARCHAR(3) NOT NULL,
    finish_position INT,
    status VARCHAR(20) NOT NULL,
    is_fastest_lap BOOLEAN DEFAULT false,
    is_partial_distance BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    amended_at TIMESTAMPTZ,
    UNIQUE(race_id, session_type, driver_code)
);

CREATE INDEX idx_sessions_race ON sessions(race_id);
CREATE INDEX idx_race_results_race ON race_results(race_id, session_type);
CREATE INDEX idx_drivers_season ON drivers(season);
