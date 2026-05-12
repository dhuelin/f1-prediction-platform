# Production compose override — image tags are substituted by deploy.yml
# Merge with docker-compose.yml: docker compose -f docker-compose.yml -f docker-compose.prod.yml up

services:
  api-gateway:
    image: IMAGE_PREFIX/api-gateway:IMAGE_TAG
    restart: unless-stopped

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
