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

`infra/init-db.sql` creates both databases on first start: `pitwall_db` (the
consolidated API) and `f1data_db` (f1-data-service).

### Stop

```bash
docker compose down        # keep volumes
docker compose down -v     # also delete postgres data
```

---

## Services

| Service | Port | Description |
|---------|------|-------------|
| api-gateway | 8080 | Spring Cloud Gateway — JWT validation, rate limiting, routes to the two backends |
| pitwall-api | 8081 | Auth, leagues, predictions, scoring, notifications and analytics |
| f1-data-service | 8085 | Race calendar, live positions (OpenF1), race results, WebSocket broadcast |

`pitwall-api` is a modular monolith. Auth, league, prediction, scoring,
notification and analytics were separate Spring Boot services until they were
consolidated; each keeps its own package under `com.f1predict.*` and its own
tables, but they share one process, one JVM and one database (`pitwall_db`).

Where a domain needs something from another, it calls a published in-process
contract rather than making an HTTP request:

| Contract | Provider | Consumer |
|----------|----------|----------|
| `prediction.api.PredictionDirectory` | `PredictionService` | scoring |
| `league.api.LeagueDirectory` | `LeagueService` | scoring |
| `scoring.api.ScoringDirectory` | `ScoringDirectoryService` | prediction, league |

f1-data-service stays a separate deployable — it owns the scheduled OpenF1
pollers and the live-race WebSocket broadcast, which have a very different
runtime profile from request/response API work.

All backend services use:
- **JWT** via `X-User-Id` header propagated by the gateway (stripped from client input — prevents header injection)
- **Flyway** for database migrations
- **RabbitMQ** for events that genuinely cross a process boundary

### Key event flows

Everything published and consumed inside `pitwall-api` is a Spring
`ApplicationEvent` — it never touches the broker:

```
prediction  →  PredictionLockedEvent   →  notification, analytics
scoring     →  StandingsUpdatedEvent   →  analytics
```

RabbitMQ now carries only what crosses the process boundary from
f1-data-service. Each consuming domain keeps its own queue: on a single shared
queue RabbitMQ would round-robin deliveries and each event would reach only one
domain.

```
f1-data-service  →  f1.events / session.complete      →  prediction, notification, analytics
f1-data-service  →  f1.events / race.result.final     →  scoring, notification, analytics
f1-data-service  →  f1.events / race.result.amended   →  scoring, notification
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
| `ci.yml` | Push / PR on any branch | Gradle tests, Docker smoke test (all services), mobile TypeScript check |
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

### Production server sizing

The VPS runs **two stacks side by side**: production (`~/f1predict`, api-gateway on
8080) and test (`~/f1predict-test`, project name `f1predict-test`, api-gateway on
8090).

**Minimum server: 4 vCPU / 8 GB (Hetzner CX32 or equivalent), plus ~4 GB swap.**

A 2 vCPU / 4 GB box is not enough and will OOM. The service Dockerfiles start the
JVM with no `-Xmx`, so without a container memory limit each JVM applies its
default `MaxRAMPercentage` of 25% *of host RAM*. Combined with
`restart: unless-stopped`, an OOM kill becomes a restart loop that starves sshd
of memory and takes SSH deploys down with it.

Both deploy overlays therefore set an explicit `mem_limit` on every container and
pass `JAVA_TOOL_OPTIONS` so each JVM sizes its heap from the cgroup limit:

| | prod | test |
|---|---|---|
| Postgres | 768m | 384m |
| Redis | 128m (`maxmemory 96mb`) | 96m (`maxmemory 64mb`) |
| RabbitMQ | 448m (watermark 0.5) | 384m (watermark 0.5) |
| api-gateway | 448m (heap 55%) | 320m (heap 50%) |
| pitwall-api | 1024m (heap 60%) | 640m (heap 60%) |
| f1-data-service | 512m (heap 60%) | 384m (heap 55%) |
| web-static | 32m | 32m |
| **Stack total** | **~3.4 GB** | **~2.2 GB** |

Consolidating six services into `pitwall-api` took each stack from nine JVMs to
three. The two stacks together now budget ~5.6 GB of the 8 GB box; before
consolidation the same two stacks needed ~8.2 GB and only fit because limits are
ceilings rather than reservations.

To give production the whole box, stop the test stack when it is not in use:

```bash
docker compose --project-name f1predict-test stop
```

Add swap if the server has none (Hetzner cloud images ship without it):

```bash
sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### Migrating an existing deployment

The consolidation replaces six databases with one. A server that already ran the
split services has data in `auth_db`, `league_db`, `prediction_db`, `scoring_db`,
`notification_db` and `analytics_db`; the new app reads `pitwall_db`, which the
deploy creates empty. **Deploying without migrating leaves that data stranded** —
the old databases are not dropped, so nothing is lost, but the app starts blank.

No table names collide across the six, so a straight dump-and-load works. Run this
on the server once, before the first consolidated deploy:

```bash
cd ~/f1predict

# 1. Back up everything first
docker exec f1predict-postgres-1 pg_dumpall -U f1predict > ~/f1predict-pre-merge.sql

# 2. Create the target database
docker exec f1predict-postgres-1 psql -U f1predict -d postgres \
  -c "CREATE DATABASE pitwall_db;"

# 3. Copy each old database's data in. Skip Flyway's own history table —
#    the consolidated app has a single renumbered V1..V11 chain of its own.
for db in auth_db league_db prediction_db scoring_db notification_db analytics_db; do
  docker exec f1predict-postgres-1 pg_dump -U f1predict --data-only \
    --exclude-table=flyway_schema_history "$db" \
  | docker exec -i f1predict-postgres-1 psql -U f1predict -d pitwall_db
done
```

Step 3 must run **after** the new app has started once and applied its migrations,
so the tables exist to load into. In practice: deploy, let `pitwall-api` come up
and create the schema, stop it, load the data, start it again.

If the deployment has no data worth keeping (only smoke-test accounts), skip all
of this — the deploy creates `pitwall_db` and Flyway builds the schema from
scratch.

---

## Design tokens

CSS custom properties in `web/src/styles/tokens.css`:
- **F1 Red:** `#E8002D` · **F1 Orange:** `#FF8000`
- Dark mode: bg `#0a0a0a`, surface `#1a1a1a`
- Light mode: bg `#f5f5f5`, surface `#ffffff`
- Exposed as Tailwind utilities (`text-f1-red`, `bg-surface`, `border-border`, etc.)

Mobile uses matching constants in `mobile/src/theme/tokens.ts`.
