# F1 Prediction Platform — Design Specification

**Date:** 2026-03-23
**Status:** Approved for implementation

---

## 1. Overview

A social F1 race prediction platform where friends compete in leagues by predicting race outcomes and earning points. Supports full race and sprint weekends, live race tracking, post-race score amendments, and fully customisable league scoring configurations.

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3, microservices architecture |
| Web Frontend | React (TypeScript), Tailwind CSS |
| Mobile | React Native (Expo), iOS + Android |
| Database | PostgreSQL (per-service schema) |
| Cache | Redis (live race state, sessions, rate limiting) |
| Messaging | RabbitMQ (async events between services) |
| API Gateway | Single entry point — auth, routing, rate limiting, SSL, WebSocket |
| F1 Data APIs | OpenF1 (live/real-time) + Jolpica-F1 (season/historical) |
| Auth | JWT tokens, Google OAuth, Apple Sign-In |
| Notifications | Push notifications (iOS APNs + Android FCM) |
| Analytics | Event-based user action tracking |

---

## 3. Microservices

### 3.1 Auth Service
- User registration (email/password), login, password reset
- Google OAuth and Apple Sign-In integration
- JWT token issuance and refresh
- Account linking (social + email accounts merged)

### 3.2 Prediction Service
- Submit and update top-N predictions per race/sprint
- Bonus bet submission with stake amounts
- Deadline enforcement — locks predictions before qualifying starts
- Validates stake does not exceed user's current points balance
- Emits `PREDICTION_LOCKED` event at deadline

### 3.3 League Service
- Create leagues (public or invite-code private)
- Join leagues (search by name or enter invite code)
- Users can belong to multiple leagues simultaneously
- Per-league scoring configuration (see Section 5)
- Mid-season catch-up: new joiners receive points equal to current league average
- League admin management (creator is default admin)

### 3.4 Scoring Service
- Calculates points for each user's prediction after race completion
- Applies the league's custom scoring configuration
- Listens for `RESULT_AMENDED` events and re-runs full scoring for affected race
- Publishes updated standings after each (re-)scoring
- Handles partial points for races stopped before 75% distance (F1 rules)

### 3.5 F1 Data Service
- Polls OpenF1 and Jolpica-F1 on a race-calendar-aware schedule
- Stores race calendar, driver list, constructor data, session schedules
- Emits session-typed events: `SESSION_COMPLETE` (with `sessionType` payload: `SPRINT_SHOOTOUT` | `QUALIFYING` | `SPRINT` | `RACE`), `LIVE_POSITION_UPDATE` (with `sessionType`), `RACE_RESULT_FINAL`, and `RESULT_AMENDED`. The Prediction Service locks sprint predictions on `SESSION_COMPLETE{sessionType: SPRINT_SHOOTOUT}` and main race predictions on `SESSION_COMPLETE{sessionType: QUALIFYING}`.
- Streams live position data to API Gateway via WebSocket during races
- Monitors for post-race amendments for 48 hours after race completion

### 3.6 Notification Service
- Push notifications to iOS (APNs) and Android (FCM)
- Pre-qualifying reminder to all users with unlocked predictions
- Race start alert
- Results published alert
- Score amended alert (when post-race penalties/DSQ change standings)
- Per-user notification preference settings

### 3.7 Analytics Service
- Tracks user action events: predictions submitted, bets placed, leagues created/joined, live dashboard views, session starts
- Independent of all other services — receives events via message queue
- No PII in raw events

---

## 4. F1 Data Polling Schedule

| Period | Frequency | Data Source | Action |
|---|---|---|---|
| Off-weekend | Daily | Jolpica | Calendar, drivers, constructors |
| Race weekend (Thu–Sun) | Every 5 min | Jolpica | Session schedules, any updates |
| Qualifying session | Every 30 sec | OpenF1 + Jolpica | Grid order; triggers prediction lock on completion |
| Race in progress | Every 5 sec | OpenF1 | Live positions → WebSocket stream |
| Post-race (0–48h) | Hourly | Jolpica + OpenF1 | Watch for classification amendments |

---

## 5. Prediction System

### 5.1 Top-N Predictions
- Users predict finishing order for the top N drivers (N is league-configurable, default 10)
- Must be submitted before qualifying begins
- Drag-to-rank UI on both web and mobile

### 5.2 Bonus Bets (optional, stake-based)
Users may optionally stake points on bonus predictions. All bets use a fixed multiplier (configurable per league, default 2x). Wrong bet = stake lost.

| Bet | Win Condition |
|---|---|
| Fastest Lap | Pick the driver who sets the fastest lap |
| DNF / DSQ / DNS | Pick one or more drivers who retire, are disqualified, or don't start |
| SC / VSC deployed | Predict yes/no |
| SC / VSC count | Predict exact number of safety car / virtual safety car deployments |

### 5.3 Prediction Deadline
- Locks **before qualifying** begins (not race start)
- Push notification reminder sent to all users with unsubmitted predictions
- No retroactive predictions accepted under any circumstances

### 5.4 Sprint Weekends
- Sprint races have a separate earlier prediction deadline — locks before the Sprint Shootout qualifying session begins (not before the sprint race itself)
- Same prediction format (top-N + bonus bets) as the main race
- Points scored separately and added to the same season total
- League admin can disable sprint scoring entirely

---

## 6. Scoring Engine

### 6.1 Top-N Proximity Scoring (fully configurable per league)

Defaults:

| Result | Points |
|---|---|
| Exact position | 10 |
| 1 position off | 7 |
| 2 positions off | 2 |
| In predicted range, outside all offset tiers | 1 |
| Outside predicted range | 0 |

**League admin can configure:**
- Prediction depth N (e.g. top 5, 10, 15, 20)
- Points for exact position
- Number of offset tiers and points per tier (e.g. 1-off=10, 2-off=6, 3-off=3, 4-off=1)
- Points for "in range but outside proximity tiers"
- Configuration changes apply from the **next race only** — never retroactively

### 6.2 Bonus Bet Scoring
- Win: receive stake × multiplier (default 2x)
- Lose: stake deducted
- Multiplier is configurable per league (1.5x / 2x / 3x / 5x)

### 6.3 Edge Cases

| Scenario | Handling |
|---|---|
| DNF mid-race | Driver excluded from final standings. Top-N prediction for that slot scores 0. DNF bet pays out if driver was picked. |
| DSQ during race | Same as DNF |
| Post-race DSQ | `RESULT_AMENDED` event fires. Full re-score of all predictions for that race across all leagues. Any open DNF/DSQ/DNS bonus bets that include that driver are paid out at this point (2x stake). |
| Post-race time penalty | Driver position changes. `RESULT_AMENDED` event fires. Full re-score applied. Positions of other affected drivers shift accordingly. |
| Post-race grid penalty (next race) | Ignored — does not affect current race results. |
| DNS (Did Not Start) | Treated identically to DNF for all scoring purposes. |
| Fastest lap amended | `RESULT_AMENDED` event fires. Fastest lap bets rescored. |
| Race < 75% distance | All points (top-N and bonus bets) halved per F1 sporting regulations. |
| Race cancelled | 0 points awarded. Stakes refunded. |
| Prediction deadline missed | 0 points for that race. No exceptions. |
| Mid-season league join | User receives points equal to the current league member average at time of joining. |

---

## 7. League Configuration

Configurable by league admin only. Changes apply from next race forward.

| Setting | Default | Options |
|---|---|---|
| Prediction depth (top N) | 10 | Any integer 1–20 |
| Exact position points | 10 | 5–25 |
| Offset tiers | 1-off=7, 2-off=2 | Admin defines number of tiers and points per tier |
| In-range (outside proximity) points | 1 | 0–5 |
| Bonus bet multiplier | 2x | 1.5x, 2x, 3x, 5x |
| Active bonus bets | All | Toggle SC/VSC, DNF, Fastest Lap individually |
| Sprint races count | Yes | Yes / No |
| Max stake per bet | Unlimited | Optional cap (admin sets) |

---

## 8. Live Race Dashboard

Shown during active race sessions. Updates via WebSocket.

- **Predicted vs Actual:** Side-by-side view of each user's predicted positions against live driver positions
- **Projected score:** Points each user would receive if the race ended at this moment
- **Live league leaderboard:** Updated in real-time as positions change
- **Orange highlight:** Any driver within 2 positions of a predicted slot (a position battle that could change a user's score) is marked in orange
- **Bonus bet trackers:** Live status of SC/VSC count, current fastest lap holder, retirement list

---

## 9. Authentication & Accounts

- Email + password registration with email verification
- Google OAuth 2.0
- Apple Sign-In
- Accounts from different providers with the same email are merged
- JWT access tokens (short-lived) + refresh tokens (long-lived, stored securely)

---

## 10. UI Design

- **Style:** Dark Racing — deep black backgrounds, F1 red (#E10600) primary accent, orange (#F97316) for live/alert states
- **Adaptive:** Full dark and light mode, follows device system preference with manual override
- **Web:** React + Tailwind CSS
- **Mobile:** React Native (Expo), platform-native navigation patterns

### Key Screens
1. Auth (login, register, social, forgot password)
2. Home Dashboard (next race countdown, prediction status, league rank snapshots, recent results)
3. Predict (drag-to-rank top-N, bonus bet entry with stake input, submit with deadline countdown)
4. Live Race Dashboard (real-time positions, projected scores, orange alerts, live leaderboard)
5. Leagues (browse public, join by code, create, per-league leaderboard + history)
6. Profile & Settings (stats, prediction history, notification prefs, dark/light toggle, account settings)

---

## 11. Agent Team

The following expert agents will execute this implementation:

| Agent | Responsibility |
|---|---|
| UX Expert | Design system, component library, screen flows, accessibility |
| Backend Java Expert | Spring Boot microservices, data models, API design, messaging |
| Frontend Expert | React web app, live dashboard, WebSocket client |
| Mobile Expert | React Native app (iOS + Android), push notifications, Expo config |
| Tester | Integration tests, scoring engine edge case tests, API contract tests |
| Devil's Advocate | Challenges assumptions, identifies edge cases, reviews decisions |

---

## 12. Development Process

### Agile Workflow
- **Backlog:** GitHub Issues — one issue per task, feature, or bug
- **Labels:** `enhancement`, `bug`, `hotfix`, `documentation`, `infrastructure`, `backend`, `frontend`, `mobile`, `testing`
- **Milestones (sprints):**
  - `Sprint 1 — Foundation` (infrastructure, auth, F1 data service)
  - `Sprint 2 — Core Predictions` (prediction service, scoring engine, league service)
  - `Sprint 3 — Web Frontend` (React app, all screens)
  - `Sprint 4 — Mobile` (React Native app, push notifications)
  - `Sprint 5 — Live Dashboard` (WebSocket, real-time scoring, live UI)
  - `Sprint 6 — Polish & Launch` (analytics, edge cases, performance, app store submission)

### Commit Convention
Every commit must reference its GitHub Issue:
```
[#12] Add JWT token issuance to Auth Service
[#34] Implement proximity scoring engine
[#47] Fix post-race DSQ re-score event handling
```

### Branching
- Feature branches: `feature/<issue-number>-<short-description>`
- Bug branches: `bugfix/<issue-number>-<short-description>`
- All branches merge to `main` via PR with `Closes #N` in description
- No direct pushes to `main`
- Production deployments triggered manually via `workflow_dispatch`

---

## 13. Out of Scope (v1)

- Real-money betting or prize systems
- Constructor predictions
- Championship predictions (season-long)
- Admin CMS / moderation tools
- Social features (comments, reactions)
