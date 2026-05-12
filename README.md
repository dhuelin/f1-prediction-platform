# F1 Prediction Platform

A social Formula 1 race prediction platform where friends compete in private leagues by predicting race outcomes and earning points. Supports top-10 finishing order predictions, bonus bets, a live race dashboard, and push notifications.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3, microservices |
| Web frontend | React 18 + TypeScript, Vite 5, Tailwind CSS v3 |
| Mobile | React Native (Expo SDK 51) — iOS & Android |
| Gateway | Spring Cloud Gateway with JWT filter + Redis rate limiting |
| Data | PostgreSQL 16 (per-service databases), Redis 7, RabbitMQ 3 |
| Real-time | STOMP over WebSocket (Spring WebSocket), Redis pub/sub |
| Live data | OpenF1 API (live positions, race control, laps) |
| Notifications | APNs (Pushy) + FCM (Firebase Admin SDK) |
| CI/CD | GitHub Actions — build, test, Docker image push, SSH deploy |
| Mobile release | EAS Build + EAS Submit (App Store + Google Play) |

---

## Local Development

### Prerequisites

- Docker Engine 25+ with the **Compose v2 plugin** (`docker compose`)

> **Note:** The legacy `docker-compose` v1 Python script is incompatible with Docker Engine 25+. Use `docker compose` (space, no hyphen). Install the plugin with `sudo apt-get install docker-compose-plugin` if needed.

### Start all services

```bash
docker compose up -d
```

Infrastructure started:

| Service | Port(s) | Credentials |
|---------|---------|-------------|
| PostgreSQL 16 | `5432` | `f1predict / f1predict` |
| Redis 7 | `6379` | — |
| RabbitMQ 3 | `5672`, `15672` (management UI) | `f1predict / f1predict` |

`infra/init-db.sql` creates all 8 service databases on first start:
`auth_db`, `f1data_db`, `prediction_db`, `league_db`, `scoring_db`, `notification_db`, `analytics_db`.

### Stop

```bash
docker compose down        # keep volumes
docker compose down -v     # also delete postgres data
```

---

## Services

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | Spring Cloud Gateway — JWT validation, rate limiting, routes to all services |
| auth-service | 8081 | Register, login, refresh, email verification, OAuth2 (Google/Apple), password reset |
| prediction-service | 8082 | Submit/update top-N predictions and bonus bets; enforces qualifying deadline |
| league-service | 8083 | Create/join leagues with invite codes, scoring config, mid-season catch-up |
| scoring-service | 8084 | Proximity scoring, bonus bet scoring, league standings, projected live scores |
| f1-data-service | 8085 | Race calendar, live positions (OpenF1), race results, WebSocket broadcast |
| notification-service | 8086 | Push notifications via APNs + FCM; device token management |
| analytics-service | 8087 | Event ingestion from RabbitMQ; participation stats; query API |

All backend services use:
- **JWT** via `X-User-Id` header propagated by the gateway (stripped from client input — prevents header injection)
- **Flyway** for database migrations
- **RabbitMQ** for event-driven communication between services

### Key event flows

```
prediction-service  →  prediction.events / prediction.locked  →  notification-service, analytics-service
f1-data-service     →  f1.events / session.complete           →  scoring-service, notification-service
f1-data-service     →  f1.events / race.result.final          →  scoring-service, notification-service, analytics-service
scoring-service     →  scoring.events / standings.updated     →  analytics-service
```

---

## Scoring Overview

**Top-N predictions (proximity scoring):**
- Exact finish (Δ=0) → 10 pts
- 1 off (|Δ|=1) → 7 pts
- 2 off (|Δ|=2) → 2 pts
- Within configured range → 1 pt

**Bonus bets:**
- FASTEST_LAP, DNF_DSQ_DNS, SC_DEPLOYED, SC_COUNT
- Win: `floor(stake × multiplier)`; Lose: `−stake`
- Multipliers configured per league

**Modifiers:**
- Partial distance race (<75%): all points halved
- Race cancelled: zero points for all users

---

## Web Frontend (`web/`)

Built with **Vite 5 + React 18 + TypeScript** (strict), **Tailwind CSS v3**.

### Running locally

```bash
cd web
cp .env.example .env       # set VITE_API_BASE_URL if needed
npm install
npm run dev                # http://localhost:5173
```

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `http://localhost:8080` | API Gateway base URL |
| `VITE_WS_BASE_URL` | derived from `VITE_API_BASE_URL` | WebSocket base (ws:// prefix) |

### Key pages

| Route | Page | Description |
|-------|------|-------------|
| `/` | Home | Next race, quick-predict CTA |
| `/predict/:raceId` | PredictPage | Drag-and-drop top-10 + bonus bets |
| `/races/:raceId/live` | LiveRaceDashboardPage | Real-time position grid via STOMP WebSocket |
| `/leagues` | LeaguesPage | League list, create / join |
| `/leagues/:id` | LeagueDetailPage | Standings, member list |

---

## Mobile App (`mobile/`)

React Native (Expo SDK 51), targeting iOS 16+ and Android API 35.

### Running locally

```bash
cd mobile
npm install
npx expo start          # scan QR code with Expo Go, or press i/a for simulator
```

| Variable | Where | Description |
|----------|-------|-------------|
| `EXPO_PUBLIC_API_URL` | `.env` | API Gateway base URL |

### Key screens

| Screen | Description |
|--------|-------------|
| HomeScreen | Next race info + navigate to live dashboard |
| PredictScreen | Submit top-10 prediction + bonus bets |
| LiveRaceScreen | Polling-based live position grid + projected scores |
| LeaguesScreen → LeagueDetailScreen | League list and standings |
| ProfileScreen | Account info, notification preferences |

The mobile app polls `/live/positions`, `/live/race-state`, and `/scores/races/{id}/projected` every 5 seconds. Polling pauses automatically when the app is in the background (`AppState`) and resumes immediately on foreground.

### Building for release

```bash
# Install EAS CLI
npm install -g eas-cli
eas login

# Development build (simulator)
eas build --platform ios --profile development

# Production build + submit
eas build --platform all --profile production --auto-submit
```

CI triggers automatically on `v*.*.*` tags via `.github/workflows/mobile-release.yml`.

---

## Performance Testing

Load tests use [k6](https://k6.io/). See [perf/README.md](perf/README.md).

```bash
k6 run perf/scripts/auth.js         # auth endpoints, 20 VUs
k6 run perf/scripts/predictions.js  # prediction submit/fetch, ramp to 50 VUs
k6 run perf/scripts/live.js         # 100 VUs polling live endpoints every 5s
```

---

## CI/CD

| Workflow | Trigger | Jobs |
|----------|---------|------|
| `ci.yml` | Push / PR on any branch | Gradle tests, Docker smoke test (all 8 services), mobile TypeScript check |
| `deploy.yml` | Push to `main` | Build + push all images to `ghcr.io`, SSH deploy, post-deploy smoke test |
| `mobile-release.yml` | Push tag `v*.*.*` | EAS Build + EAS Submit for iOS and Android |

### Required GitHub Secrets

| Secret | Used by |
|--------|---------|
| `DEPLOY_SSH_KEY` | `deploy.yml` — SSH private key for production server |
| `DEPLOY_KNOWN_HOSTS` | `deploy.yml` — known_hosts entry for production server |
| `DEPLOY_USER` | `deploy.yml` — SSH username |
| `DEPLOY_HOST` | `deploy.yml` — production server hostname |
| `JWT_SECRET` | `deploy.yml` (via prod compose) |
| `APNS_TEAM_ID`, `APNS_KEY_ID`, `APNS_AUTH_KEY`, `APNS_BUNDLE_ID` | push notifications |
| `FIREBASE_CREDENTIALS_JSON` | FCM push notifications |
| `EXPO_TOKEN` | `mobile-release.yml` — EAS authentication |
| `APPLE_APP_SPECIFIC_PASSWORD` | `mobile-release.yml` — App Store Connect |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_KEY` | `mobile-release.yml` — Google Play |

---

## Design tokens

CSS custom properties in `web/src/styles/tokens.css`:
- **F1 Red:** `#E8002D` · **F1 Orange:** `#FF8000`
- Dark mode: bg `#0a0a0a`, surface `#1a1a1a`
- Light mode: bg `#f5f5f5`, surface `#ffffff`
- Exposed as Tailwind utilities (`text-f1-red`, `bg-surface`, `border-border`, etc.)

Mobile uses matching constants in `mobile/src/theme/tokens.ts`.
