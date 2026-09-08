-- Databases created on first Postgres start.
-- pitwall_db backs the consolidated API (auth, league, prediction, scoring,
-- notification and analytics tables all live here); f1data_db backs the
-- separately deployed f1-data-service.
CREATE DATABASE pitwall_db;
CREATE DATABASE f1data_db;
