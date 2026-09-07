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
#   postgres     320m
#   redis         96m
#   rabbitmq     384m
#   8 services × 320m = 2560m
#   web-static    32m
#   ────────────────────
#   test total  3392m
#
# Prod (~4.8 GB) + test (~3.4 GB) exceeds 8 GB if every container simultaneously
# hits its ceiling, which in practice they do not (steady state is ~5.5-6 GB).
# Stop the test stack when it is not in use to give production the whole box:
#   docker compose --project-name f1predict-test stop

services:
  # Give infra more time to start on VPS cold boot
  postgres:
    restart: unless-stopped
    mem_limit: 320m
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
    # In test, only wait for Redis; backend services use service_started
    # to avoid deadlock when Spring Boot takes 2-3 min on cold start
    depends_on:
      redis:
        condition: service_healthy
      auth-service:
        condition: service_started
      prediction-service:
        condition: service_started
      league-service:
        condition: service_started
      scoring-service:
        condition: service_started
      f1-data-service:
        condition: service_started
      notification-service:
        condition: service_started

  auth-service:
    image: IMAGE_PREFIX/auth-service:IMAGE_TAG
    restart: unless-stopped
    mem_limit: 320m
    environment:
      JWT_SECRET: "${JWT_SECRET}"
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -XX:MaxMetaspaceSize=112m -Xss512k"
      GOOGLE_CLIENT_ID: "${GOOGLE_CLIENT_ID}"
      GOOGLE_CLIENT_SECRET: "${GOOGLE_CLIENT_SECRET}"
      APPLE_CLIENT_ID: "${APPLE_CLIENT_ID}"
      APPLE_TEAM_ID: "${APPLE_TEAM_ID}"
      APPLE_KEY_ID: "${APPLE_KEY_ID}"

  prediction-service:
    image: IMAGE_PREFIX/prediction-service:IMAGE_TAG
    restart: unless-stopped
    mem_limit: 320m
    environment:
      JWT_SECRET: "${JWT_SECRET}"
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -XX:MaxMetaspaceSize=112m -Xss512k"

  league-service:
    image: IMAGE_PREFIX/league-service:IMAGE_TAG
    restart: unless-stopped
    mem_limit: 320m
    environment:
      JWT_SECRET: "${JWT_SECRET}"
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -XX:MaxMetaspaceSize=112m -Xss512k"

  scoring-service:
    image: IMAGE_PREFIX/scoring-service:IMAGE_TAG
    restart: unless-stopped
    mem_limit: 320m
    environment:
      JWT_SECRET: "${JWT_SECRET}"
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -XX:MaxMetaspaceSize=112m -Xss512k"

  f1-data-service:
    image: IMAGE_PREFIX/f1-data-service:IMAGE_TAG
    restart: unless-stopped
    mem_limit: 320m
    environment:
      JWT_SECRET: "${JWT_SECRET}"
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -XX:MaxMetaspaceSize=112m -Xss512k"

  notification-service:
    image: IMAGE_PREFIX/notification-service:IMAGE_TAG
    restart: unless-stopped
    mem_limit: 320m
    environment:
      JWT_SECRET: "${JWT_SECRET}"
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -XX:MaxMetaspaceSize=112m -Xss512k"
      APNS_TEAM_ID: "${APNS_TEAM_ID}"
      APNS_KEY_ID: "${APNS_KEY_ID}"
      APNS_BUNDLE_ID: "${APNS_BUNDLE_ID}"
      APNS_AUTH_KEY: "${APNS_AUTH_KEY}"
      APNS_PRODUCTION: "false"
      FIREBASE_CREDENTIALS_JSON: "${FIREBASE_CREDENTIALS_JSON}"

  analytics-service:
    image: IMAGE_PREFIX/analytics-service:IMAGE_TAG
    restart: unless-stopped
    mem_limit: 320m
    environment:
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -XX:MaxMetaspaceSize=112m -Xss512k"

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
