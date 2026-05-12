# Performance Tests

Load tests for the F1 Prediction Platform using [k6](https://k6.io/).

## Prerequisites

```bash
# macOS
brew install k6

# Windows (Chocolatey)
choco install k6

# Docker
docker run --rm -i grafana/k6 run - < scripts/auth.js
```

## Running

```bash
# Auth endpoints
k6 run perf/scripts/auth.js

# Prediction submit / fetch
k6 run perf/scripts/predictions.js

# Live race polling
k6 run perf/scripts/live.js

# All suites (sequential)
k6 run perf/scripts/auth.js && k6 run perf/scripts/predictions.js && k6 run perf/scripts/live.js
```

Set `BASE_URL` to override the default `http://localhost:8080`:

```bash
k6 run -e BASE_URL=https://api.staging.f1predict.app perf/scripts/auth.js
```

## Thresholds

Each script enforces:
- p95 response time < 500 ms
- Error rate < 1 %
