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

## Web Frontend (`web/`)

Built with **Vite 5 + React + TypeScript** (strict mode), **Tailwind CSS v3**.

### Stack
| Layer | Technology |
|-------|-----------|
| Build | Vite 5, TypeScript strict |
| UI | React 18, Tailwind CSS v3 |
| Routing | React Router v6 (`createBrowserRouter`) |
| State | Zustand (auth store, theme store) |
| HTTP | Axios with JWT interceptor + refresh logic |
| DnD | @hello-pangea/dnd |
| Utilities | clsx, tailwind-merge, date-fns |

### Running locally

```bash
cd web
cp .env.example .env          # set VITE_API_BASE_URL if needed
npm install
npm run dev                   # http://localhost:5173
```

### Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `http://localhost:8080` | API Gateway base URL |

### Project structure

```
web/src/
  api/           # Axios client + typed API functions (auth, predictions, leagues, f1data, scoring)
  components/ui/ # Design-system components (Button, Input, Card, Badge, Avatar, Loader, Modal, ThemeToggle)
  hooks/         # useTheme
  lib/           # cn() utility (clsx + tailwind-merge)
  pages/         # Route-level page components
  router/        # createBrowserRouter config + ProtectedRoute
  store/         # Zustand stores (authStore, themeStore)
  styles/        # tokens.css — CSS custom properties for colours, spacing, shadows
```

### Design tokens

Defined as CSS custom properties in `src/styles/tokens.css`:
- **F1 Red:** `#E8002D` · **F1 Orange:** `#FF8000`
- Dark mode: bg `#0a0a0a`, surface `#1a1a1a`
- Light mode: bg `#f5f5f5`, surface `#ffffff`
- Exposed as Tailwind utilities (`text-f1-red`, `bg-surface`, `border-border`, etc.)
