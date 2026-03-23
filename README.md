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

See `docs/superpowers/plans/` for sprint implementation plans.
