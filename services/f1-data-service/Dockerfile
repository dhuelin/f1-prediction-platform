FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY common/build.gradle common/
COPY common/src common/src
COPY services/f1-data-service/build.gradle services/f1-data-service/
COPY services/f1-data-service/src services/f1-data-service/src
RUN chmod +x gradlew && ./gradlew :services:f1-data-service:bootJar --no-daemon -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/services/f1-data-service/build/libs/*.jar app.jar
EXPOSE 8085
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5   CMD wget -qO- http://localhost:8085/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
