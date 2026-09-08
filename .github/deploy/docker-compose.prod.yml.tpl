# Production compose override — image tags are substituted by deploy.yml
# Merge with docker-compose.yml: docker compose -f docker-compose.yml -f docker-compose.prod.yml up
#
# ── Memory budget (Hetzner CX32: 4 vCPU / 8 GB) ───────────────────────────────
# Every container has an explicit mem_limit. Without one the JVM sees the whole
# host and defaults MaxRAMPercentage to 25% of it, which is what OOM-killed this
# server in production.
#
#   postgres         768m
#   redis            128m
#   rabbitmq         448m
#   api-gateway      448m
#   pitwall-api     1024m   (auth + league + prediction + scoring + notification + analytics)
#   f1-data-service  512m
#   web-static        32m
#   ────────────────────────
#   prod total      3360m   (test stack budgets a further ~2.2 GB — see test tpl)
#
# Consolidating six services into pitwall-api took this stack from nine JVMs to
# three, which is why the per-container limits can now be generous and the two
# stacks together still fit in 8 GB with room to spare.
#
# mem_limit is a CEILING, not a reservation. The limits are here so a single
# runaway JVM is killed alone instead of taking the host down, and so each JVM
# sizes its heap from the cgroup rather than from host RAM.

services:
  # RabbitMQ is slow to start on production VPS — give it more time
  rabbitmq:
    restart: unless-stopped
    mem_limit: 448m
    environment:
      # Relative to the cgroup limit above, not to host RAM
      RABBITMQ_VM_MEMORY_HIGH_WATERMARK: "0.5"
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 30s
      timeout: 10s
      retries: 10
      start_period: 60s

  # Postgres healthcheck — generous for cold start
  postgres:
    restart: unless-stopped
    mem_limit: 768m
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U f1predict"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  redis:
    restart: unless-stopped
    mem_limit: 128m
    command:
      - redis-server
      - --maxmemory
      - 96mb
      - --maxmemory-policy
      - allkeys-lru

  api-gateway:
    image: IMAGE_PREFIX/api-gateway:IMAGE_TAG
    restart: unless-stopped
    mem_limit: 448m
    ports:
      - "8080:8080"
    environment:
      JWT_SECRET: "${JWT_SECRET}"
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=55 -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Xss512k"
    # Backends use service_started to avoid deadlock when Spring Boot takes
    # 2-3 min to pass healthchecks on cold start
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
    mem_limit: 1024m
    environment:
      JWT_SECRET: "${JWT_SECRET}"
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:MaxMetaspaceSize=192m -Xss512k"
      GOOGLE_CLIENT_ID: "${GOOGLE_CLIENT_ID}"
      GOOGLE_CLIENT_SECRET: "${GOOGLE_CLIENT_SECRET}"
      APPLE_CLIENT_ID: "${APPLE_CLIENT_ID}"
      APPLE_TEAM_ID: "${APPLE_TEAM_ID}"
      APPLE_KEY_ID: "${APPLE_KEY_ID}"
      APNS_TEAM_ID: "${APNS_TEAM_ID}"
      APNS_KEY_ID: "${APNS_KEY_ID}"
      APNS_BUNDLE_ID: "${APNS_BUNDLE_ID}"
      APNS_AUTH_KEY: "${APNS_AUTH_KEY}"
      APNS_PRODUCTION: "true"
      FIREBASE_CREDENTIALS_JSON: "${FIREBASE_CREDENTIALS_JSON}"

  f1-data-service:
    image: IMAGE_PREFIX/f1-data-service:IMAGE_TAG
    restart: unless-stopped
    mem_limit: 512m
    environment:
      JWT_SECRET: "${JWT_SECRET}"
      JAVA_TOOL_OPTIONS: "-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Xss512k"

  # Pitwall launch page — served from nginx:alpine mounting the pitwall-launch gh-pages clone
  # The api-gateway routes /** to this container so pitwall.guru/ shows the landing page
  web-static:
    image: nginx:alpine
    restart: unless-stopped
    mem_limit: 32m
    volumes:
      - ./web-static:/usr/share/nginx/html:ro

  # Full web app is dev-only (React/Vite build served locally); excluded from VPS
  web:
    profiles: ["dev"]
