FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY common/build.gradle common/
COPY common/src common/src
COPY services/auth-service/build.gradle services/auth-service/
COPY services/auth-service/src services/auth-service/src
RUN chmod +x gradlew && ./gradlew :services:auth-service:bootJar --no-daemon -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/services/auth-service/build/libs/*.jar app.jar
EXPOSE 8081
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5   CMD wget -qO- http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
