# Production compose override — image tags are substituted by deploy.yml
# Merge with docker-compose.yml: docker compose -f docker-compose.yml -f docker-compose.prod.yml up

services:
  # RabbitMQ is slow to start on production VPS — give it more time
  rabbitmq:
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 30s
      timeout: 10s
      retries: 10
      start_period: 60s

  # Postgres healthcheck — generous for cold start
  postgres:
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U f1predict"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  api-gateway:
    image: IMAGE_PREFIX/api-gateway:IMAGE_TAG
    restart: unless-stopped
    # In production, only wait for Redis (fast); backend services use service_started
    # to avoid deadlock when Spring Boot takes 2-3 min to pass healthchecks on cold start
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
    environment:
      JWT_SECRET: "${JWT_SECRET}"
      GOOGLE_CLIENT_ID: "${GOOGLE_CLIENT_ID}"
      GOOGLE_CLIENT_SECRET: "${GOOGLE_CLIENT_SECRET}"
      APPLE_CLIENT_ID: "${APPLE_CLIENT_ID}"
      APPLE_TEAM_ID: "${APPLE_TEAM_ID}"
      APPLE_KEY_ID: "${APPLE_KEY_ID}"

  prediction-service:
    image: IMAGE_PREFIX/prediction-service:IMAGE_TAG
    restart: unless-stopped
    environment:
      JWT_SECRET: "${JWT_SECRET}"

  league-service:
    image: IMAGE_PREFIX/league-service:IMAGE_TAG
    restart: unless-stopped
    environment:
      JWT_SECRET: "${JWT_SECRET}"

  scoring-service:
    image: IMAGE_PREFIX/scoring-service:IMAGE_TAG
    restart: unless-stopped
    environment:
      JWT_SECRET: "${JWT_SECRET}"

  f1-data-service:
    image: IMAGE_PREFIX/f1-data-service:IMAGE_TAG
    restart: unless-stopped
    environment:
      JWT_SECRET: "${JWT_SECRET}"

  notification-service:
    image: IMAGE_PREFIX/notification-service:IMAGE_TAG
    restart: unless-stopped
    environment:
      JWT_SECRET: "${JWT_SECRET}"
      APNS_TEAM_ID: "${APNS_TEAM_ID}"
      APNS_KEY_ID: "${APNS_KEY_ID}"
      APNS_BUNDLE_ID: "${APNS_BUNDLE_ID}"
      APNS_AUTH_KEY: "${APNS_AUTH_KEY}"
      APNS_PRODUCTION: "true"
      FIREBASE_CREDENTIALS_JSON: "${FIREBASE_CREDENTIALS_JSON}"

  analytics-service:
    image: IMAGE_PREFIX/analytics-service:IMAGE_TAG
    restart: unless-stopped

  # Pitwall launch page — served from nginx:alpine mounting the pitwall-launch gh-pages clone
  # The api-gateway routes /** to this container so pitwall.guru/ shows the landing page
  web-static:
    image: nginx:alpine
    restart: unless-stopped
    volumes:
      - ./web-static:/usr/share/nginx/html:ro

  # Full web app is dev-only (React/Vite build served locally); excluded from VPS
  web:
    profiles: ["dev"]
