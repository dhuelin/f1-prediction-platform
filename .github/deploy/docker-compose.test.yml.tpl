# Test compose override — image tags substituted by deploy.yml, project-name f1predict-test
# Merge: docker compose --project-name f1predict-test -f docker-compose.yml -f docker-compose.test.yml up
#
# Base docker-compose.yml has NO host port bindings — all services talk via Docker network.
# This overlay only needs to expose api-gateway on 8090 so nginx can reach it from the host.
# Infra (postgres, redis, rabbitmq) is internal to the f1predict-test Docker network.
#
# ── Memory budget (shares the CX32 with the production stack) ─────────────────
# The test stack is deliberately smaller than prod — it exists for smoke tests,
# not load. Limits are ceilings, not reservations.
#
#   postgres         384m
#   redis             96m
#   rabbitmq         384m
#   api-gateway      320m
#   pitwall-api      640m
#   f1-data-service  384m
#   web-static        32m
#   ────────────────────────
#   test total      2240m
#
# Prod (~3.4 GB) + test (~2.2 GB) is ~5.6 GB of the 8 GB box, so both stacks now
# fit comfortably — before consolidation the same two stacks needed ~8.2 GB. Stop
# the test stack when it is not in use to give production the whole machine:
#   docker compose --project-name f1predict-test stop

services:
  postgres:
    restart: unless-stopped
    mem_limit: 384m
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U f1predict"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  redis:
    restart: unless-stopped
    mem_limit: 96m
    command:
      - redis-server
      - --maxmemory
      - 64mb
      - --maxmemory-policy
      - allkeys-lru

  rabbitmq:
    restart: unless-stopped
    mem_limit: 384m
    environment:
      RABBITMQ_VM_MEMORY_HIGH_WATERMARK: "0.5"
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 30s
      timeout: 10s
      retries: 10
      start_period: 60s

  api-gateway:
    image: IMAGE_PREFIX/api-gateway:IMAGE_TAG
    restart: unless-stopped
    mem_limit: 320m
    ports:
      - "8090:8080"
    environment:
      JWT_SECRET: "${JWT_SECRET}"
      SPRING_DATA_REDIS_HOST: redis
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -XX:MaxMetaspaceSize=112m -Xss512k"
    depends_on:
      redis:
        condition: service_healthy
      pitwall-api:
        condition: service_started
      f1-data-service:
        condition: service_started

  pitwall-api:
    image: IMAGE_PREFIX/pitwall-api:IMAGE_TAG
    restart: unless-stopped
    mem_limit: 640m
    environment:
      JWT_SECRET: "${JWT_SECRET}"
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:MaxMetaspaceSize=160m -Xss512k"
      GOOGLE_CLIENT_ID: "${GOOGLE_CLIENT_ID}"
      GOOGLE_CLIENT_SECRET: "${GOOGLE_CLIENT_SECRET}"
      APPLE_CLIENT_ID: "${APPLE_CLIENT_ID}"
      APPLE_TEAM_ID: "${APPLE_TEAM_ID}"
      APPLE_KEY_ID: "${APPLE_KEY_ID}"
      APNS_TEAM_ID: "${APNS_TEAM_ID}"
      APNS_KEY_ID: "${APNS_KEY_ID}"
      APNS_BUNDLE_ID: "${APNS_BUNDLE_ID}"
      APNS_AUTH_KEY: "${APNS_AUTH_KEY}"
      APNS_PRODUCTION: "false"
      FIREBASE_CREDENTIALS_JSON: "${FIREBASE_CREDENTIALS_JSON}"

  f1-data-service:
    image: IMAGE_PREFIX/f1-data-service:IMAGE_TAG
    restart: unless-stopped
    mem_limit: 384m
    environment:
      JWT_SECRET: "${JWT_SECRET}"
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=55 -XX:+UseSerialGC -XX:MaxMetaspaceSize=112m -Xss512k"

  # Pitwall launch page — served from nginx:alpine, volume-mounted from pitwall-launch gh-pages clone
  web-static:
    image: nginx:alpine
    restart: unless-stopped
    mem_limit: 32m
    volumes:
      - ./web-static:/usr/share/nginx/html:ro

  # Full web app is dev-only; excluded from VPS
  web:
    profiles: ["dev"]
