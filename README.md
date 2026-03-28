# F1 Prediction Platform

A social F1 race prediction platform where friends compete in leagues by predicting race outcomes and earning points.

## Tech Stack
- **Backend:** Java 21, Spring Boot 3, microservices
- **Web:** React (TypeScript), Tailwind CSS
- **Mobile:** React Native (Expo) — iOS & Android
- **Infra:** PostgreSQL, Redis, RabbitMQ, Spring Cloud Gateway

## Local Development

### Prerequisites
- Docker and Docker Compose

### Spin up local dependencies

```bash
docker-compose up -d
```

This starts:
| Service   | Port(s)        | Credentials          |
|-----------|----------------|----------------------|
| PostgreSQL 16 | `5432`     | `f1predict / f1predict` |
| Redis 7   | `6379`         | —                    |
| RabbitMQ 3 | `5672`, `15672` (management UI) | `f1predict / f1predict` |

On first startup, `infra/init-db.sql` creates the 7 service databases:
`auth_db`, `f1data_db`, `prediction_db`, `league_db`, `scoring_db`, `notification_db`, `analytics_db`.

### Stop

```bash
docker-compose down
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | Spring Cloud Gateway — JWT validation, routes to all services |
| auth-service | 8081 | Registration, login, JWT issuance |
| prediction-service | 8082 | Submit/update top-N predictions and bonus bets; enforces lock deadline |
| league-service | 8083 | Create/join leagues, manage scoring config, mid-season catch-up |
| scoring-service | 8084 | Proximity scoring engine, bonus bet scoring, league standings |
| f1-data-service | 8085 | Race calendar, live session data, race results |

## Scoring Overview

- **Top-N predictions:** exact finish = 10pts, 1-off = 7pts, 2-off = 2pts, in-range = 1pt
- **Bonus bets:** FASTEST_LAP, DNF_DSQ_DNS, SC_DEPLOYED, SC_COUNT — win = floor(stake × multiplier), lose = −stake
- **Partial distance (&lt;75%):** all points halved
- **Race cancelled:** zero points for all users

See `docs/superpowers/plans/` for sprint implementation plans.
