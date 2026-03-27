package com.f1predict.prediction;

import com.f1predict.prediction.repository.PredictionRepository;
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
class PredictionControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", () -> "localhost");
        registry.add("spring.rabbitmq.port", () -> "5672");
    }

    @MockBean
    RabbitTemplate rabbitTemplate;

    @Autowired MockMvc mockMvc;
    @Autowired PredictionRepository predictionRepository;

    private final UUID userId = UUID.randomUUID();
    private final String raceId = "2026-01";

    @BeforeEach
    void setUp() {
        predictionRepository.deleteAll();
    }

    @Test
    void submit_withValidRequest_returns201() throws Exception {
        mockMvc.perform(post("/predictions/" + raceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                    {"rankedDriverCodes":["VER","HAM","LEC","NOR","PIA","RUS","ALO","SAI","GAS","HUL"]}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.locked").value(false))
            .andExpect(jsonPath("$.rankedDriverCodes[0]").value("VER"));
    }

    @Test
    void submit_duplicate_returns409() throws Exception {
        String body = """
            {"rankedDriverCodes":["VER","HAM","LEC","NOR","PIA","RUS","ALO","SAI","GAS","HUL"]}
            """;
        mockMvc.perform(post("/predictions/" + raceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content(body))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/predictions/" + raceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content(body))
            .andExpect(status().isConflict());
    }

    @Test
    void update_replacesPrediction() throws Exception {
        mockMvc.perform(post("/predictions/" + raceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                    {"rankedDriverCodes":["VER","HAM","LEC","NOR","PIA","RUS","ALO","SAI","GAS","HUL"]}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(put("/predictions/" + raceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                    {"rankedDriverCodes":["HAM","VER","LEC","NOR","PIA","RUS","ALO","SAI","GAS","HUL"]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rankedDriverCodes[0]").value("HAM"));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        mockMvc.perform(put("/predictions/" + raceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                    {"rankedDriverCodes":["VER","HAM","LEC","NOR","PIA","RUS","ALO","SAI","GAS","HUL"]}
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    void submitBet_withValidPrediction_returns201() throws Exception {
        // First create prediction
        mockMvc.perform(post("/predictions/" + raceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                    {"rankedDriverCodes":["VER","HAM","LEC","NOR","PIA","RUS","ALO","SAI","GAS","HUL"]}
                    """))
            .andExpect(status().isCreated());

        // Then submit bet
        mockMvc.perform(post("/predictions/" + raceId + "/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                    {"betType":"FASTEST_LAP","stake":50,"betValue":"VER"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.betType").value("FASTEST_LAP"))
            .andExpect(jsonPath("$.stake").value(50));
    }

    @Test
    void submitBet_noPrediction_returns404() throws Exception {
        mockMvc.perform(post("/predictions/" + raceId + "/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                    {"betType":"FASTEST_LAP","stake":50,"betValue":"VER"}
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    void submitBet_invalidBetType_returns400() throws Exception {
        // First create prediction
        mockMvc.perform(post("/predictions/" + raceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                    {"rankedDriverCodes":["VER","HAM","LEC","NOR","PIA","RUS","ALO","SAI","GAS","HUL"]}
                    """))
            .andExpect(status().isCreated());

        // Submit bet with invalid type
        mockMvc.perform(post("/predictions/" + raceId + "/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                    {"betType":"INVALID_TYPE","stake":50,"betValue":"VER"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void lockedPrediction_rejectsUpdate() throws Exception {
        // Submit prediction
        mockMvc.perform(post("/predictions/" + raceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                    {"rankedDriverCodes":["VER","HAM","LEC","NOR","PIA","RUS","ALO","SAI","GAS","HUL"]}
                    """))
            .andExpect(status().isCreated());

        // Lock it manually
        predictionRepository.findByRaceId(raceId).forEach(p -> {
            p.setLocked(true);
            predictionRepository.save(p);
        });

        // Update should fail with 409
        mockMvc.perform(put("/predictions/" + raceId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", userId.toString())
                .content("""
                    {"rankedDriverCodes":["HAM","VER","LEC","NOR","PIA","RUS","ALO","SAI","GAS","HUL"]}
                    """))
            .andExpect(status().isConflict());
    }
}
