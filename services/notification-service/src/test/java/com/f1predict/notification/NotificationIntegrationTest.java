package com.f1predict.notification;

import com.f1predict.notification.repository.DeviceTokenRepository;
import com.f1predict.notification.repository.NotificationPreferencesRepository;
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
    @Autowired NotificationPreferencesRepository preferencesRepository;

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
}
