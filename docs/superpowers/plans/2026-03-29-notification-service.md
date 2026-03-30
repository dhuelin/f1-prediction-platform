# Notification Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Notification Service — a Spring Boot microservice that registers device tokens, stores per-user preferences, and sends push notifications (APNs + FCM) in response to RabbitMQ domain events.

**Architecture:** The service is a standalone Spring Boot module wired into the existing Gradle multi-project build. It listens to four RabbitMQ events (`prediction.locked`, `session.complete`, `race.result.final`, `race.result.amended`) and dispatches push notifications to registered device tokens via a `PushProvider` abstraction (APNs for iOS, FCM for Android). Push credentials are optional at startup — if absent the providers log a warning and no-op, so tests and local dev work without real credentials.

**Tech Stack:** Java 21, Spring Boot 3.3, Spring AMQP, Spring Data JPA, PostgreSQL, Flyway, `com.eatthepath:pushy` (APNs), `com.google.firebase:firebase-admin` (FCM), Testcontainers (integration tests).

---

## File Structure

### Created
| File | Responsibility |
|---|---|
| `services/notification-service/build.gradle` | Gradle module dependencies |
| `services/notification-service/Dockerfile` | Multi-stage Docker build |
| `services/notification-service/src/main/resources/application.yml` | Spring config |
| `.../notification/NotificationServiceApplication.java` | Spring Boot entry point |
| `.../notification/config/RabbitMQConfig.java` | Exchange + queue + binding declarations |
| `.../db/migration/V1__create_notification_schema.sql` | `device_tokens` + `notification_preferences` tables |
| `.../notification/model/DeviceToken.java` | JPA entity |
| `.../notification/model/NotificationPreferences.java` | JPA entity |
| `.../notification/repository/DeviceTokenRepository.java` | Spring Data repo |
| `.../notification/repository/NotificationPreferencesRepository.java` | Spring Data repo |
| `.../notification/controller/NotificationController.java` | POST/DELETE device tokens + GET/PUT preferences |
| `.../notification/push/PushPayload.java` | Record: title, body, data map |
| `.../notification/push/PushProvider.java` | Interface: `send(List<String> tokens, PushPayload)` |
| `.../notification/push/ApnsPushProvider.java` | Pushy-based APNs implementation |
| `.../notification/push/FcmPushProvider.java` | Firebase Admin SDK FCM implementation |
| `.../notification/push/PushDispatcher.java` | Routes device tokens to correct provider |
| `.../notification/service/NotificationService.java` | Business logic: look up tokens, filter prefs, dispatch |
| `.../notification/listener/NotificationEventListener.java` | `@RabbitListener` for all four events |
| `.../notification/NotificationIntegrationTest.java` | Integration tests with Testcontainers |

### Modified
| File | Change |
|---|---|
| `settings.gradle` | Add `include 'services:notification-service'` |
| `gradle/libs.versions.toml` | Add `pushy` and `firebase-admin` dependencies |
| `docker-compose.yml` | Add `notification-service` entry (port 8086) |
| `api-gateway/src/main/resources/application.yml` | Add `/notifications/**` route |
| `.github/workflows/ci.yml` | Add `APNS_TEAM_ID`, `APNS_KEY_ID`, `APNS_BUNDLE_ID` test env vars |

---

## Task 1: Project skeleton

**GitHub Issue:** Closes #123

**Files:**
- Create: `services/notification-service/build.gradle`
- Create: `services/notification-service/Dockerfile`
- Create: `services/notification-service/src/main/resources/application.yml`
- Create: `services/notification-service/src/main/java/com/f1predict/notification/NotificationServiceApplication.java`
- Modify: `settings.gradle`
- Modify: `gradle/libs.versions.toml`
- Modify: `docker-compose.yml`
- Modify: `api-gateway/src/main/resources/application.yml`
- Modify: `.github/workflows/ci.yml`

- [ ] **Step 1: Register module in settings.gradle**

Add after the last `include` line:
```gradle
include 'services:notification-service'
```

- [ ] **Step 2: Add pushy and firebase-admin to libs.versions.toml**

Add to `[versions]`:
```toml
pushy = "0.15.4"
firebase-admin = "9.3.0"
```

Add to `[libraries]`:
```toml
pushy = { module = "com.eatthepath:pushy", version.ref = "pushy" }
firebase-admin = { module = "com.google.firebase:firebase-admin", version.ref = "firebase-admin" }
```

- [ ] **Step 3: Create build.gradle**

```gradle
// services/notification-service/build.gradle
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation project(':common')
    implementation libs.spring.boot.actuator
    implementation libs.spring.boot.web
    implementation libs.spring.boot.validation
    implementation libs.spring.boot.data.jpa
    implementation libs.spring.boot.amqp
    implementation libs.flyway
    implementation libs.flyway.postgres
    implementation libs.pushy
    implementation libs.firebase.admin
    runtimeOnly libs.postgresql
    testImplementation libs.spring.boot.test
    testImplementation libs.testcontainers.junit
    testImplementation libs.testcontainers.postgresql
}
```

- [ ] **Step 4: Create application.yml**

```yaml
# services/notification-service/src/main/resources/application.yml
server:
  port: 8086

spring:
  application:
    name: notification-service
  datasource:
    url: jdbc:postgresql://localhost:5432/notification_db
    username: f1predict
    password: f1predict
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    locations: classpath:db/migration
  rabbitmq:
    host: localhost
    port: 5672
    username: f1predict
    password: f1predict
  autoconfigure:
    exclude: org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

apns:
  team-id: ${APNS_TEAM_ID:}
  key-id: ${APNS_KEY_ID:}
  bundle-id: ${APNS_BUNDLE_ID:com.f1predict.app}
  auth-key: ${APNS_AUTH_KEY:}         # full content of .p8 file
  production: ${APNS_PRODUCTION:false}

fcm:
  credentials-json: ${FIREBASE_CREDENTIALS_JSON:}  # service account JSON string
```

- [ ] **Step 5: Create main application class**

```java
// services/notification-service/src/main/java/com/f1predict/notification/NotificationServiceApplication.java
package com.f1predict.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
```

- [ ] **Step 6: Create Dockerfile**

```dockerfile
# services/notification-service/Dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY common/build.gradle common/
COPY common/src common/src
COPY services/notification-service/build.gradle services/notification-service/
COPY services/notification-service/src services/notification-service/src
RUN chmod +x gradlew && ./gradlew :services:notification-service:bootJar --no-daemon -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/services/notification-service/build/libs/*.jar app.jar
EXPOSE 8086
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD wget -qO- http://localhost:8086/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 7: Add notification-service to docker-compose.yml**

Add after `scoring-service` entry (pattern follows exactly the existing services):
```yaml
  notification-service:
    build:
      context: .
      dockerfile: services/notification-service/Dockerfile
    ports:
      - "8086:8086"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/notification_db
      SPRING_DATASOURCE_USERNAME: f1predict
      SPRING_DATASOURCE_PASSWORD: f1predict
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_USERNAME: f1predict
      SPRING_RABBITMQ_PASSWORD: f1predict
      JWT_SECRET: change-me-in-production-use-256-bit-key-minimum
      APNS_TEAM_ID: ""
      APNS_KEY_ID: ""
      APNS_BUNDLE_ID: com.f1predict.app
      APNS_AUTH_KEY: ""
      FIREBASE_CREDENTIALS_JSON: ""
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8086/actuator/health"]
      interval: 15s
      timeout: 5s
      start_period: 90s
      retries: 5
```

- [ ] **Step 8: Add gateway route to api-gateway/src/main/resources/application.yml**

In the `spring.cloud.gateway.routes` list, add after the last existing route:
```yaml
        - id: notification-service
          uri: http://notification-service:8086
          predicates:
            - Path=/notifications/**
```

- [ ] **Step 9: Add test env vars to .github/workflows/ci.yml**

In the `build-and-test` job's `Run all unit tests` step, add to the `env:` block:
```yaml
          APNS_TEAM_ID: test
          APNS_KEY_ID: test
          APNS_BUNDLE_ID: com.f1predict.app
```

Also in the `docker-smoke-test` job's `wait_healthy` section, add **before** the `api-gateway` line:
```bash
wait_healthy notification-service 8086 || exit 1
```

And add to the `check_route` list:
```bash
check_route "/notifications" "http://localhost:8080/notifications/health"  || exit 1
```

And update the `api-gateway` entry's `depends_on` block in `docker-compose.yml` to add:
```yaml
      notification-service:
        condition: service_healthy
```

- [ ] **Step 10: Verify the Gradle build resolves**

```bash
./gradlew :services:notification-service:compileJava --no-daemon
```

Expected: BUILD SUCCESSFUL (no source files yet, that's fine)

- [ ] **Step 11: Commit**

```bash
git add settings.gradle gradle/libs.versions.toml services/notification-service/build.gradle \
  services/notification-service/Dockerfile \
  services/notification-service/src/main/resources/application.yml \
  services/notification-service/src/main/java/com/f1predict/notification/NotificationServiceApplication.java \
  docker-compose.yml api-gateway/src/main/resources/application.yml \
  .github/workflows/ci.yml
git commit -m "feat: notification-service project skeleton [#123]"
```

---

## Task 2: Flyway schema

**GitHub Issue:** Closes #124

**Files:**
- Create: `services/notification-service/src/main/resources/db/migration/V1__create_notification_schema.sql`

- [ ] **Step 1: Write the migration**

```sql
-- services/notification-service/src/main/resources/db/migration/V1__create_notification_schema.sql
CREATE TABLE device_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID         NOT NULL,
    token       VARCHAR(512) NOT NULL,
    platform    VARCHAR(10)  NOT NULL CHECK (platform IN ('APNS', 'FCM')),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_token UNIQUE (user_id, token)
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);

CREATE TABLE notification_preferences (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              UUID    NOT NULL UNIQUE,
    prediction_reminder  BOOLEAN NOT NULL DEFAULT TRUE,
    race_start           BOOLEAN NOT NULL DEFAULT TRUE,
    results_published    BOOLEAN NOT NULL DEFAULT TRUE,
    score_amended        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW()
);
```

- [ ] **Step 2: Write the failing migration test**

```java
// services/notification-service/src/test/java/com/f1predict/notification/MigrationTest.java
package com.f1predict.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class MigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.rabbitmq.host", () -> "localhost");
        r.add("spring.rabbitmq.port", () -> "5672");
    }

    // Spring AMQP uses lazy connections by default in Spring Boot 3, so no real broker is needed.
    // Mocking RabbitTemplate prevents any accidental publish attempts during the test.
    @MockBean org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Autowired JdbcTemplate jdbc;

    @Test
    void deviceTokensTableExists() {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'device_tokens'",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void notificationPreferencesTableExists() {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'notification_preferences'",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
```

- [ ] **Step 3: Run test — expect FAIL**

```bash
./gradlew :services:notification-service:test --tests "*.MigrationTest" --no-daemon
```

Expected: FAIL — the application context fails to start because there are no JPA `@Entity` classes yet. Flyway will run the migration (tables are created), but Hibernate has nothing to validate against. The error will be something like `No entity classes scanned` or a context startup failure. This is expected — proceed to Task 3 where entities are added.

- [ ] **Step 4: Commit schema only**

```bash
git add services/notification-service/src/main/resources/db/migration/
git commit -m "feat: notification-service Flyway schema [#124]"
```

---

## Task 3: DeviceToken entity + registration endpoint

**GitHub Issue:** Closes #123 (partial — device token CRUD)

**Files:**
- Create: `services/notification-service/src/main/java/com/f1predict/notification/model/DeviceToken.java`
- Create: `services/notification-service/src/main/java/com/f1predict/notification/repository/DeviceTokenRepository.java`
- Create: `services/notification-service/src/main/java/com/f1predict/notification/controller/NotificationController.java` (partial — device token endpoints only)
- Create: `services/notification-service/src/test/java/com/f1predict/notification/NotificationIntegrationTest.java` (partial)

- [ ] **Step 1: Write the failing test**

```java
// services/notification-service/src/test/java/com/f1predict/notification/NotificationIntegrationTest.java
package com.f1predict.notification;

import com.f1predict.notification.repository.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class NotificationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.rabbitmq.host", () -> "localhost");
        r.add("spring.rabbitmq.port", () -> "5672");
    }

    @MockBean RabbitTemplate rabbitTemplate;

    @Autowired MockMvc mockMvc;
    @Autowired DeviceTokenRepository tokenRepository;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        preferencesRepository.deleteAll();
    }

    @Test
    void registerToken_returns201_andPersists() throws Exception {
        mockMvc.perform(post("/notifications/devices")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"token":"abc123","platform":"FCM"}
                    """))
            .andExpect(status().isCreated());

        assertThat(tokenRepository.findByUserId(userId)).hasSize(1);
    }

    @Test
    void registerToken_duplicate_isIdempotent() throws Exception {
        String body = """{"token":"abc123","platform":"FCM"}""";
        mockMvc.perform(post("/notifications/devices")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/notifications/devices")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());

        assertThat(tokenRepository.findByUserId(userId)).hasSize(1);
    }

    @Test
    void deleteToken_removes_fromDb() throws Exception {
        mockMvc.perform(post("/notifications/devices")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"delete-me","platform":"APNS"}"""))
            .andExpect(status().isCreated());

        mockMvc.perform(delete("/notifications/devices/delete-me")
                .header("X-User-Id", userId.toString()))
            .andExpect(status().isNoContent());

        assertThat(tokenRepository.findByUserId(userId)).isEmpty();
    }
}
```

- [ ] **Step 2: Run to confirm FAIL**

```bash
./gradlew :services:notification-service:test --tests "*.NotificationIntegrationTest" --no-daemon
```

Expected: FAIL — `No qualifying bean of type 'DeviceTokenRepository'`

- [ ] **Step 3: Create DeviceToken entity**

```java
// services/notification-service/src/main/java/com/f1predict/notification/model/DeviceToken.java
package com.f1predict.notification.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_tokens")
public class DeviceToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Platform platform;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public enum Platform { APNS, FCM }

    protected DeviceToken() {}

    public DeviceToken(UUID userId, String token, Platform platform) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public Platform getPlatform() { return platform; }
}
```

- [ ] **Step 4: Create DeviceTokenRepository**

```java
// services/notification-service/src/main/java/com/f1predict/notification/repository/DeviceTokenRepository.java
package com.f1predict.notification.repository;

import com.f1predict.notification.model.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    List<DeviceToken> findByUserId(UUID userId);
    Optional<DeviceToken> findByUserIdAndToken(UUID userId, String token);
    // findAll() is inherited from JpaRepository — do not redeclare it
}
```

- [ ] **Step 5: Create NotificationController (device token endpoints only)**

```java
// services/notification-service/src/main/java/com/f1predict/notification/controller/NotificationController.java
package com.f1predict.notification.controller;

import com.f1predict.notification.model.DeviceToken;
import com.f1predict.notification.repository.DeviceTokenRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final DeviceTokenRepository tokenRepository;

    public NotificationController(DeviceTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    record RegisterTokenRequest(
        @NotBlank String token,
        @Pattern(regexp = "APNS|FCM") String platform
    ) {}

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerToken(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody RegisterTokenRequest req) {
        // Idempotent: only insert if not already present
        tokenRepository.findByUserIdAndToken(userId, req.token()).ifPresentOrElse(
            existing -> {}, // already exists, no-op
            () -> tokenRepository.save(
                new DeviceToken(userId, req.token(),
                    DeviceToken.Platform.valueOf(req.platform())))
        );
    }

    @DeleteMapping("/devices/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeToken(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable String token) {
        tokenRepository.findByUserIdAndToken(userId, token)
            .ifPresent(tokenRepository::delete);
    }
}
```

- [ ] **Step 6: Run test — expect PASS**

```bash
./gradlew :services:notification-service:test --tests "*.NotificationIntegrationTest#registerToken*" \
  --tests "*.NotificationIntegrationTest#deleteToken*" --no-daemon
```

Expected: PASS for all 3 device token tests

- [ ] **Step 7: Commit**

```bash
git add services/notification-service/src/
git commit -m "feat: notification-service device token registration [#123]"
```

---

## Task 4: NotificationPreferences CRUD

**GitHub Issue:** Closes #129

**Files:**
- Create: `services/notification-service/src/main/java/com/f1predict/notification/model/NotificationPreferences.java`
- Create: `services/notification-service/src/main/java/com/f1predict/notification/repository/NotificationPreferencesRepository.java`
- Modify: `services/notification-service/src/main/java/com/f1predict/notification/controller/NotificationController.java` (add preferences endpoints)
- Modify: `services/notification-service/src/test/java/com/f1predict/notification/NotificationIntegrationTest.java` (add preferences tests)

- [ ] **Step 1: Write the failing tests — add to NotificationIntegrationTest**

Add these tests to `NotificationIntegrationTest`:
```java
    @Autowired NotificationPreferencesRepository preferencesRepository;

    @Test
    void getPreferences_returnsDefaults_whenNotSet() throws Exception {
        mockMvc.perform(get("/notifications/preferences")
                .header("X-User-Id", userId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.predictionReminder").value(true))
            .andExpect(jsonPath("$.raceStart").value(true))
            .andExpect(jsonPath("$.resultsPublished").value(true))
            .andExpect(jsonPath("$.scoreAmended").value(true));
    }

    @Test
    void putPreferences_updatesAndReturns() throws Exception {
        mockMvc.perform(put("/notifications/preferences")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"predictionReminder":false,"raceStart":true,
                     "resultsPublished":true,"scoreAmended":false}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.predictionReminder").value(false))
            .andExpect(jsonPath("$.scoreAmended").value(false));

        // Verify persisted
        var stored = preferencesRepository.findByUserId(userId);
        assertThat(stored).isPresent();
        assertThat(stored.get().isPredictionReminder()).isFalse();
    }
```

- [ ] **Step 2: Run to confirm FAIL**

```bash
./gradlew :services:notification-service:test --tests "*.NotificationIntegrationTest#get*" \
  --tests "*.NotificationIntegrationTest#put*" --no-daemon
```

Expected: FAIL — 404 (endpoints not defined yet)

- [ ] **Step 3: Create NotificationPreferences entity**

```java
// services/notification-service/src/main/java/com/f1predict/notification/model/NotificationPreferences.java
package com.f1predict.notification.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreferences {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false) private boolean predictionReminder = true;
    @Column(nullable = false) private boolean raceStart = true;
    @Column(nullable = false) private boolean resultsPublished = true;
    @Column(nullable = false) private boolean scoreAmended = true;

    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();

    protected NotificationPreferences() {}

    public NotificationPreferences(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() { return userId; }
    public boolean isPredictionReminder() { return predictionReminder; }
    public boolean isRaceStart() { return raceStart; }
    public boolean isResultsPublished() { return resultsPublished; }
    public boolean isScoreAmended() { return scoreAmended; }

    public void update(boolean predictionReminder, boolean raceStart,
                       boolean resultsPublished, boolean scoreAmended) {
        this.predictionReminder = predictionReminder;
        this.raceStart = raceStart;
        this.resultsPublished = resultsPublished;
        this.scoreAmended = scoreAmended;
        this.updatedAt = Instant.now();
    }
}
```

- [ ] **Step 4: Create NotificationPreferencesRepository**

```java
// services/notification-service/src/main/java/com/f1predict/notification/repository/NotificationPreferencesRepository.java
package com.f1predict.notification.repository;

import com.f1predict.notification.model.NotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, Long> {
    Optional<NotificationPreferences> findByUserId(UUID userId);
}
```

- [ ] **Step 5: Add preferences endpoints to NotificationController**

Add these fields and endpoints to `NotificationController`:

```java
    // Add to constructor:
    private final NotificationPreferencesRepository preferencesRepository;

    public NotificationController(DeviceTokenRepository tokenRepository,
                                  NotificationPreferencesRepository preferencesRepository) {
        this.tokenRepository = tokenRepository;
        this.preferencesRepository = preferencesRepository;
    }

    record PreferencesResponse(boolean predictionReminder, boolean raceStart,
                               boolean resultsPublished, boolean scoreAmended) {}

    record UpdatePreferencesRequest(boolean predictionReminder, boolean raceStart,
                                    boolean resultsPublished, boolean scoreAmended) {}

    @GetMapping("/preferences")
    public PreferencesResponse getPreferences(@RequestHeader("X-User-Id") UUID userId) {
        var prefs = preferencesRepository.findByUserId(userId)
            .orElse(new NotificationPreferences(userId));
        return new PreferencesResponse(
            prefs.isPredictionReminder(), prefs.isRaceStart(),
            prefs.isResultsPublished(), prefs.isScoreAmended());
    }

    @PutMapping("/preferences")
    public PreferencesResponse updatePreferences(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody UpdatePreferencesRequest req) {
        var prefs = preferencesRepository.findByUserId(userId)
            .orElseGet(() -> new NotificationPreferences(userId));
        prefs.update(req.predictionReminder(), req.raceStart(),
                     req.resultsPublished(), req.scoreAmended());
        preferencesRepository.save(prefs);
        return new PreferencesResponse(
            prefs.isPredictionReminder(), prefs.isRaceStart(),
            prefs.isResultsPublished(), prefs.isScoreAmended());
    }
```

Don't forget to add the import for `NotificationPreferences` at the top.

- [ ] **Step 6: Run all tests — expect PASS**

```bash
./gradlew :services:notification-service:test --no-daemon
```

Expected: All tests pass

- [ ] **Step 7: Commit**

```bash
git add services/notification-service/src/
git commit -m "feat: notification preferences CRUD [#129]"
```

---

## Task 5: Push provider abstraction (APNs + FCM)

**GitHub Issue:** Closes #123 (provider setup)

**Files:**
- Create: `services/notification-service/src/main/java/com/f1predict/notification/push/PushPayload.java`
- Create: `services/notification-service/src/main/java/com/f1predict/notification/push/PushProvider.java`
- Create: `services/notification-service/src/main/java/com/f1predict/notification/push/ApnsPushProvider.java`
- Create: `services/notification-service/src/main/java/com/f1predict/notification/push/FcmPushProvider.java`
- Create: `services/notification-service/src/main/java/com/f1predict/notification/push/PushDispatcher.java`
- Create: `services/notification-service/src/main/java/com/f1predict/notification/config/PushConfig.java`

- [ ] **Step 1: Create PushPayload**

```java
// services/notification-service/src/main/java/com/f1predict/notification/push/PushPayload.java
package com.f1predict.notification.push;

import java.util.Map;

public record PushPayload(
    String title,
    String body,
    Map<String, String> data   // e.g. {"type":"RESULTS_PUBLISHED","raceId":"2026-05","leagueId":"abc"}
) {}
```

- [ ] **Step 2: Create PushProvider interface**

```java
// services/notification-service/src/main/java/com/f1predict/notification/push/PushProvider.java
package com.f1predict.notification.push;

import java.util.List;

public interface PushProvider {
    /** Send a push notification to one or more device tokens. Fire-and-forget. */
    void send(List<String> tokens, PushPayload payload);
}
```

- [ ] **Step 3: Create ApnsPushProvider**

```java
// services/notification-service/src/main/java/com/f1predict/notification/push/ApnsPushProvider.java
package com.f1predict.notification.push;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ApnsPushProvider implements PushProvider {

    private static final Logger log = LoggerFactory.getLogger(ApnsPushProvider.class);

    private final ApnsClient client;
    private final String bundleId;

    public ApnsPushProvider(ApnsClient client, String bundleId) {
        this.client = client;
        this.bundleId = bundleId;
    }

    @Override
    public void send(List<String> tokens, PushPayload payload) {
        String apnsPayload = new SimpleApnsPayloadBuilder()
            .setAlertTitle(payload.title())
            .setAlertBody(payload.body())
            .build();

        for (String token : tokens) {
            SimpleApnsPushNotification notification =
                new SimpleApnsPushNotification(token, bundleId, apnsPayload);
            // Pushy 0.15 returns a CompletableFuture — use whenComplete for async result handling
            client.sendNotification(notification).whenComplete((response, ex) -> {
                if (ex != null) {
                    log.error("APNs send failed for token {}", token, ex);
                } else if (!response.isAccepted()) {
                    log.warn("APNs rejected token {}: {}", token,
                        response.getRejectionReason().orElse("unknown"));
                }
            });
        }
    }
}
```

- [ ] **Step 4: Create FcmPushProvider**

```java
// services/notification-service/src/main/java/com/f1predict/notification/push/FcmPushProvider.java
package com.f1predict.notification.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FcmPushProvider implements PushProvider {

    private static final Logger log = LoggerFactory.getLogger(FcmPushProvider.class);

    private final FirebaseMessaging messaging;

    public FcmPushProvider(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }

    @Override
    public void send(List<String> tokens, PushPayload payload) {
        Notification notification = Notification.builder()
            .setTitle(payload.title())
            .setBody(payload.body())
            .build();

        for (String token : tokens) {
            Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putAllData(payload.data())
                .build();
            messaging.sendAsync(message).whenComplete((msgId, ex) -> {
                if (ex != null) {
                    log.error("FCM send failed for token {}", token, ex);
                }
            });
        }
    }
}
```

- [ ] **Step 5: Create PushDispatcher**

```java
// services/notification-service/src/main/java/com/f1predict/notification/push/PushDispatcher.java
package com.f1predict.notification.push;

import com.f1predict.notification.model.DeviceToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PushDispatcher {

    private final PushProvider apnsProvider;
    private final PushProvider fcmProvider;

    public PushDispatcher(PushProvider apnsProvider, PushProvider fcmProvider) {
        this.apnsProvider = apnsProvider;
        this.fcmProvider = fcmProvider;
    }

    public void dispatch(List<DeviceToken> tokens, PushPayload payload) {
        Map<DeviceToken.Platform, List<String>> byPlatform = tokens.stream()
            .collect(Collectors.groupingBy(
                DeviceToken::getPlatform,
                Collectors.mapping(DeviceToken::getToken, Collectors.toList())
            ));

        List<String> apnsTokens = byPlatform.getOrDefault(DeviceToken.Platform.APNS, List.of());
        List<String> fcmTokens  = byPlatform.getOrDefault(DeviceToken.Platform.FCM, List.of());

        if (!apnsTokens.isEmpty()) apnsProvider.send(apnsTokens, payload);
        if (!fcmTokens.isEmpty())  fcmProvider.send(fcmTokens, payload);
    }
}
```

- [ ] **Step 6: Create PushConfig — wires providers from application properties**

```java
// services/notification-service/src/main/java/com/f1predict/notification/config/PushConfig.java
package com.f1predict.notification.config;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.f1predict.notification.push.ApnsPushProvider;
import com.f1predict.notification.push.FcmPushProvider;
import com.f1predict.notification.push.PushProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class PushConfig {

    private static final Logger log = LoggerFactory.getLogger(PushConfig.class);

    @Value("${apns.team-id:}") private String apnsTeamId;
    @Value("${apns.key-id:}") private String apnsKeyId;
    @Value("${apns.bundle-id:com.f1predict.app}") private String apnsBundleId;
    @Value("${apns.auth-key:}") private String apnsAuthKey;
    @Value("${apns.production:false}") private boolean apnsProduction;
    @Value("${fcm.credentials-json:}") private String fcmCredentialsJson;

    @Bean(name = "apnsProvider")
    public PushProvider apnsProvider() {
        if (apnsTeamId.isBlank() || apnsKeyId.isBlank() || apnsAuthKey.isBlank()) {
            log.warn("APNs credentials not configured — APNs push notifications disabled");
            return (tokens, payload) ->
                log.info("[APNS no-op] Would send '{}' to {} token(s)", payload.title(), tokens.size());
        }
        try {
            ApnsClient client = new ApnsClientBuilder()
                .setApnsServer(apnsProduction
                    ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                    : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                .setSigningKey(
                    com.eatthepath.pushy.apns.auth.ApnsSigningKey.loadFromInputStream(
                        new ByteArrayInputStream(apnsAuthKey.getBytes(StandardCharsets.UTF_8)),
                        apnsTeamId, apnsKeyId))
                .build();
            return new ApnsPushProvider(client, apnsBundleId);
        } catch (Exception e) {
            log.error("Failed to initialise APNs client — falling back to no-op", e);
            return (tokens, payload) -> {};
        }
    }

    @Bean(name = "fcmProvider")
    public PushProvider fcmProvider() {
        if (fcmCredentialsJson.isBlank()) {
            log.warn("Firebase credentials not configured — FCM push notifications disabled");
            return (tokens, payload) ->
                log.info("[FCM no-op] Would send '{}' to {} token(s)", payload.title(), tokens.size());
        }
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(fcmCredentialsJson.getBytes(StandardCharsets.UTF_8)));
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                ? FirebaseApp.initializeApp(options)
                : FirebaseApp.getInstance();
            return new FcmPushProvider(FirebaseMessaging.getInstance(app));
        } catch (Exception e) {
            log.error("Failed to initialise FCM client — falling back to no-op", e);
            return (tokens, payload) -> {};
        }
    }
}
```

- [ ] **Step 7: Verify build still passes**

```bash
./gradlew :services:notification-service:test --no-daemon
```

Expected: All existing tests still pass

- [ ] **Step 8: Commit**

```bash
git add services/notification-service/src/main/java/com/f1predict/notification/push/ \
        services/notification-service/src/main/java/com/f1predict/notification/config/PushConfig.java
git commit -m "feat: push provider abstraction — APNs (pushy) + FCM (firebase-admin) [#123]"
```

---

## Task 6: RabbitMQ config + event listeners + notification service

**GitHub Issues:** Closes #125 #126 #127 #128

**Files:**
- Create: `services/notification-service/src/main/java/com/f1predict/notification/config/RabbitMQConfig.java`
- Create: `services/notification-service/src/main/java/com/f1predict/notification/service/NotificationService.java`
- Create: `services/notification-service/src/main/java/com/f1predict/notification/listener/NotificationEventListener.java`
- Modify: `services/notification-service/src/test/java/com/f1predict/notification/NotificationIntegrationTest.java`

- [ ] **Step 1: Write failing tests — add to NotificationIntegrationTest**

```java
    @MockBean com.f1predict.notification.push.PushDispatcher pushDispatcher;

    @Test
    void onPredictionLocked_sendsReminderToUsersWithPref() {
        // Register a token for our test user
        tokenRepository.save(new DeviceToken(userId, "tok1",
            DeviceToken.Platform.FCM));

        var event = new com.f1predict.common.events.PredictionLockedEvent(
            "2026-05", com.f1predict.common.events.SessionCompleteEvent.SessionType.QUALIFYING, 42);

        // Simulate the listener receiving the event
        notificationService.sendPredictionReminder(event.raceId());

        org.mockito.Mockito.verify(pushDispatcher, org.mockito.Mockito.times(1))
            .dispatch(org.mockito.Mockito.anyList(),
                org.mockito.Mockito.argThat(p -> p.title().contains("Predictions locked")));
    }

    @Autowired com.f1predict.notification.service.NotificationService notificationService;

    @Test
    void onPredictionLocked_skipsUsersWithPrefDisabled() {
        // Save token AND disable prediction_reminder
        tokenRepository.save(new DeviceToken(userId, "tok2", DeviceToken.Platform.APNS));
        var prefs = new com.f1predict.notification.model.NotificationPreferences(userId);
        prefs.update(false, true, true, true);
        preferencesRepository.save(prefs);

        notificationService.sendPredictionReminder("2026-05");

        org.mockito.Mockito.verify(pushDispatcher, org.mockito.Mockito.never())
            .dispatch(org.mockito.Mockito.anyList(), org.mockito.Mockito.any());
    }
```

- [ ] **Step 2: Run to confirm FAIL**

```bash
./gradlew :services:notification-service:test \
  --tests "*.NotificationIntegrationTest#onPrediction*" --no-daemon
```

Expected: FAIL — `NotificationService` bean not found

- [ ] **Step 3: Create RabbitMQConfig**

```java
// services/notification-service/src/main/java/com/f1predict/notification/config/RabbitMQConfig.java
package com.f1predict.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Upstream exchanges
    public static final String PREDICTION_EXCHANGE   = "prediction.events";
    public static final String F1_EXCHANGE           = "f1.events";

    // Routing keys to subscribe to
    public static final String PREDICTION_LOCKED_KEY = "prediction.locked";
    public static final String SESSION_COMPLETE_KEY  = "session.complete";
    public static final String RACE_RESULT_KEY       = "race.result.final";
    public static final String RESULT_AMENDED_KEY    = "race.result.amended";

    // This service's queues
    public static final String PREDICTION_LOCKED_QUEUE   = "notification-service.prediction-locked";
    public static final String SESSION_COMPLETE_QUEUE    = "notification-service.session-complete";
    public static final String RACE_RESULT_QUEUE         = "notification-service.race-result-final";
    public static final String RESULT_AMENDED_QUEUE      = "notification-service.result-amended";

    // Dead-letter
    public static final String DLX                       = "notification-service.dlx";

    @Bean public TopicExchange predictionExchange() {
        return new TopicExchange(PREDICTION_EXCHANGE, true, false);
    }
    @Bean public TopicExchange f1Exchange() {
        return new TopicExchange(F1_EXCHANGE, true, false);
    }
    @Bean public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    private Queue durableQueue(String name) {
        return QueueBuilder.durable(name)
            .withArgument("x-dead-letter-exchange", DLX)
            .withArgument("x-dead-letter-routing-key", name)
            .build();
    }

    @Bean public Queue predictionLockedQueue()  { return durableQueue(PREDICTION_LOCKED_QUEUE); }
    @Bean public Queue sessionCompleteQueue()   { return durableQueue(SESSION_COMPLETE_QUEUE); }
    @Bean public Queue raceResultQueue()        { return durableQueue(RACE_RESULT_QUEUE); }
    @Bean public Queue resultAmendedQueue()     { return durableQueue(RESULT_AMENDED_QUEUE); }

    @Bean public Queue predictionLockedDlq() { return new Queue(PREDICTION_LOCKED_QUEUE + ".dlq", true); }
    @Bean public Queue sessionCompleteDlq()  { return new Queue(SESSION_COMPLETE_QUEUE  + ".dlq", true); }
    @Bean public Queue raceResultDlq()       { return new Queue(RACE_RESULT_QUEUE        + ".dlq", true); }
    @Bean public Queue resultAmendedDlq()    { return new Queue(RESULT_AMENDED_QUEUE     + ".dlq", true); }

    @Bean public Binding predictionLockedBinding(Queue predictionLockedQueue, TopicExchange predictionExchange) {
        return BindingBuilder.bind(predictionLockedQueue).to(predictionExchange).with(PREDICTION_LOCKED_KEY);
    }
    @Bean public Binding sessionCompleteBinding(Queue sessionCompleteQueue, TopicExchange f1Exchange) {
        return BindingBuilder.bind(sessionCompleteQueue).to(f1Exchange).with(SESSION_COMPLETE_KEY);
    }
    @Bean public Binding raceResultBinding(Queue raceResultQueue, TopicExchange f1Exchange) {
        return BindingBuilder.bind(raceResultQueue).to(f1Exchange).with(RACE_RESULT_KEY);
    }
    @Bean public Binding resultAmendedBinding(Queue resultAmendedQueue, TopicExchange f1Exchange) {
        return BindingBuilder.bind(resultAmendedQueue).to(f1Exchange).with(RESULT_AMENDED_KEY);
    }

    @Bean public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(messageConverter());
        return t;
    }
}
```

- [ ] **Step 4: Create NotificationService**

```java
// services/notification-service/src/main/java/com/f1predict/notification/service/NotificationService.java
package com.f1predict.notification.service;

import com.f1predict.notification.model.DeviceToken;
import com.f1predict.notification.model.NotificationPreferences;
import com.f1predict.notification.push.PushDispatcher;
import com.f1predict.notification.push.PushPayload;
import com.f1predict.notification.repository.DeviceTokenRepository;
import com.f1predict.notification.repository.NotificationPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final DeviceTokenRepository tokenRepository;
    private final NotificationPreferencesRepository prefsRepository;
    private final PushDispatcher pushDispatcher;

    public NotificationService(DeviceTokenRepository tokenRepository,
                               NotificationPreferencesRepository prefsRepository,
                               PushDispatcher pushDispatcher) {
        this.tokenRepository = tokenRepository;
        this.prefsRepository = prefsRepository;
        this.pushDispatcher = pushDispatcher;
    }

    // Called when predictions are locked (PREDICTION_LOCKED event)
    public void sendPredictionReminder(String raceId) {
        List<DeviceToken> tokens = getTokensForPref(NotificationPreferences::isPredictionReminder);
        if (tokens.isEmpty()) return;
        log.info("Sending prediction reminder for race {} to {} token(s)", raceId, tokens.size());
        pushDispatcher.dispatch(tokens, new PushPayload(
            "Predictions locked",
            "Predictions are now locked for race " + raceId + ". Good luck!",
            Map.of("type", "PREDICTION_REMINDER", "raceId", raceId)
        ));
    }

    // Called when qualifying session completes — race is coming up
    public void sendRaceStartAlert(String raceId) {
        List<DeviceToken> tokens = getTokensForPref(NotificationPreferences::isRaceStart);
        if (tokens.isEmpty()) return;
        log.info("Sending race start alert for race {} to {} token(s)", raceId, tokens.size());
        pushDispatcher.dispatch(tokens, new PushPayload(
            "Race day!",
            "Race " + raceId + " is starting. Check your predictions!",
            Map.of("type", "RACE_START", "raceId", raceId)
        ));
    }

    // Called when final race result is published
    public void sendResultsPublished(String raceId) {
        List<DeviceToken> tokens = getTokensForPref(NotificationPreferences::isResultsPublished);
        if (tokens.isEmpty()) return;
        log.info("Sending results published for race {} to {} token(s)", raceId, tokens.size());
        pushDispatcher.dispatch(tokens, new PushPayload(
            "Results are in!",
            "Final results for race " + raceId + " are published. See how you scored.",
            Map.of("type", "RESULTS_PUBLISHED", "raceId", raceId)
        ));
    }

    // Called when a result is amended post-race
    public void sendScoreAmended(String raceId, String reason) {
        List<DeviceToken> tokens = getTokensForPref(NotificationPreferences::isScoreAmended);
        if (tokens.isEmpty()) return;
        log.info("Sending score amended for race {} reason={} to {} token(s)", raceId, reason, tokens.size());
        pushDispatcher.dispatch(tokens, new PushPayload(
            "Scores updated",
            "Race " + raceId + " results were amended (" + reason + "). Your score may have changed.",
            Map.of("type", "SCORE_AMENDED", "raceId", raceId)
        ));
    }

    /**
     * Returns all device tokens for users who have the given preference enabled
     * (defaulting to enabled if no preferences row exists for that user).
     */
    private List<DeviceToken> getTokensForPref(Predicate<NotificationPreferences> prefCheck) {
        return tokenRepository.findAll().stream()
            .filter(token -> {
                UUID uid = token.getUserId();
                return prefsRepository.findByUserId(uid)
                    .map(prefCheck::test)
                    .orElse(true); // default: enabled
            })
            .toList();
    }
}
```

- [ ] **Step 5: Create NotificationEventListener**

```java
// services/notification-service/src/main/java/com/f1predict/notification/listener/NotificationEventListener.java
package com.f1predict.notification.listener;

import com.f1predict.common.events.*;
import com.f1predict.notification.config.RabbitMQConfig;
import com.f1predict.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.PREDICTION_LOCKED_QUEUE)
    public void onPredictionLocked(PredictionLockedEvent event) {
        log.info("PredictionLocked: race={} session={}", event.raceId(), event.sessionType());
        notificationService.sendPredictionReminder(event.raceId());
    }

    @RabbitListener(queues = RabbitMQConfig.SESSION_COMPLETE_QUEUE)
    public void onSessionComplete(SessionCompleteEvent event) {
        // Only race start alert when qualifying is done (race starts soon after)
        if (event.sessionType() == SessionCompleteEvent.SessionType.QUALIFYING) {
            log.info("SessionComplete(QUALIFYING): race={} — sending race start alert", event.raceId());
            notificationService.sendRaceStartAlert(event.raceId());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.RACE_RESULT_QUEUE)
    public void onRaceResultFinal(RaceResultFinalEvent event) {
        log.info("RaceResultFinal: race={} session={}", event.raceId(), event.sessionType());
        notificationService.sendResultsPublished(event.raceId());
    }

    @RabbitListener(queues = RabbitMQConfig.RESULT_AMENDED_QUEUE)
    public void onResultAmended(ResultAmendedEvent event) {
        log.info("ResultAmended: race={} reason={}", event.raceId(), event.amendmentReason());
        notificationService.sendScoreAmended(event.raceId(), event.amendmentReason());
    }
}
```

- [ ] **Step 6: Run all tests — expect PASS**

```bash
./gradlew :services:notification-service:test --no-daemon
```

Expected: All tests pass

- [ ] **Step 7: Commit**

```bash
git add services/notification-service/src/main/java/com/f1predict/notification/
git commit -m "feat: notification event listeners and push dispatch [#125][#126][#127][#128]"
```

---

## Task 7: Integration test — end-to-end notification flow

**Files:**
- Modify: `services/notification-service/src/test/java/com/f1predict/notification/NotificationIntegrationTest.java`

- [ ] **Step 1: Add remaining integration tests**

Add these tests to `NotificationIntegrationTest`:
```java
    @Test
    void onRaceResultFinal_sendsResultsPublished() {
        tokenRepository.save(new DeviceToken(userId, "tok3", DeviceToken.Platform.FCM));

        notificationService.sendResultsPublished("2026-06");

        org.mockito.Mockito.verify(pushDispatcher).dispatch(
            org.mockito.Mockito.anyList(),
            org.mockito.Mockito.argThat(p -> p.title().equals("Results are in!")
                && p.data().get("type").equals("RESULTS_PUBLISHED")));
    }

    @Test
    void onResultAmended_sendsScoreAmended() {
        tokenRepository.save(new DeviceToken(userId, "tok4", DeviceToken.Platform.APNS));

        notificationService.sendScoreAmended("2026-06", "POST_RACE_DSQ");

        org.mockito.Mockito.verify(pushDispatcher).dispatch(
            org.mockito.Mockito.anyList(),
            org.mockito.Mockito.argThat(p -> p.title().equals("Scores updated")
                && p.data().get("raceId").equals("2026-06")));
    }

    @Test
    void noTokensRegistered_noPushDispatched() {
        // tokenRepository is empty after setUp()
        notificationService.sendResultsPublished("2026-07");
        org.mockito.Mockito.verify(pushDispatcher, org.mockito.Mockito.never())
            .dispatch(org.mockito.Mockito.anyList(), org.mockito.Mockito.any());
    }

    @Test
    void raceStartAlert_onlyOnQualifyingSessionComplete() {
        tokenRepository.save(new DeviceToken(userId, "tok5", DeviceToken.Platform.FCM));

        // RACE session type should NOT trigger race start alert
        var eventListener = new com.f1predict.notification.listener.NotificationEventListener(notificationService);
        eventListener.onSessionComplete(new SessionCompleteEvent(
            "2026-06", SessionCompleteEvent.SessionType.RACE, "2026", 6));

        org.mockito.Mockito.verify(pushDispatcher, org.mockito.Mockito.never())
            .dispatch(org.mockito.Mockito.anyList(), org.mockito.Mockito.any());
    }
```

- [ ] **Step 2: Run all tests — expect PASS**

```bash
./gradlew :services:notification-service:test --no-daemon
```

Expected: All tests pass

- [ ] **Step 3: Run the full build to confirm no regressions**

```bash
./gradlew test --no-daemon -q
```

Expected: BUILD SUCCESSFUL — all modules pass

- [ ] **Step 4: Commit**

```bash
git add services/notification-service/src/test/
git commit -m "test: notification service integration tests [#123][#124][#125][#126][#127][#128][#129]"
```

---

## Branch and PR

The entire sprint is implemented on a single branch.

```bash
# Before starting any task:
git checkout main && git pull origin main
git checkout -b feature/123-notification-service

# After all tasks complete:
git push origin feature/123-notification-service
gh pr create --title "feat: Sprint 5 — Notification Service (push notifications)" \
  --body "$(cat <<'EOF'
## Summary
- New `notification-service` Spring Boot module on port 8086
- APNs (iOS) and FCM (Android) push provider abstraction with no-op fallback when credentials absent
- Device token registration: `POST/DELETE /notifications/devices`
- Per-user notification preferences: `GET/PUT /notifications/preferences`
- RabbitMQ listeners for prediction-locked, session-complete, race-result-final, result-amended events
- All services wired: settings.gradle, docker-compose, api-gateway routes, CI

## Test plan
- [ ] `./gradlew :services:notification-service:test` — all tests pass
- [ ] `./gradlew test` — full build passes
- [ ] Docker: `docker compose up notification-service` — health check returns 200

Closes #123 #124 #125 #126 #127 #128 #129
EOF
)"
```
