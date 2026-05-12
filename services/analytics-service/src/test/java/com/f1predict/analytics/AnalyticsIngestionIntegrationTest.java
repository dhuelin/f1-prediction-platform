package com.f1predict.analytics;

import com.f1predict.analytics.listener.AnalyticsEventListener;
import com.f1predict.analytics.repository.PredictionParticipationStatRepository;
import com.f1predict.analytics.repository.RaceAnalyticsEventRepository;
import com.f1predict.common.events.PredictionLockedEvent;
import com.f1predict.common.events.SessionCompleteEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AnalyticsIngestionIntegrationTest {

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

    @MockBean RabbitTemplate rabbitTemplate;

    @Autowired MockMvc mockMvc;
    @Autowired AnalyticsEventListener listener;
    @Autowired RaceAnalyticsEventRepository eventRepo;
    @Autowired PredictionParticipationStatRepository participationRepo;

    @BeforeEach
    void setUp() {
        participationRepo.deleteAll();
        eventRepo.deleteAll();
    }

    @Test
    void onPredictionLocked_persistsRawEventAndParticipationStat() {
        var event = new PredictionLockedEvent("race-001", SessionCompleteEvent.SessionType.QUALIFYING, 42);

        listener.onPredictionLocked(event);

        var events = eventRepo.findByRaceIdOrderByOccurredAtAsc("race-001");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("PREDICTION_LOCKED");
        assertThat(events.get(0).getRaceId()).isEqualTo("race-001");

        var stat = participationRepo.findByRaceId("race-001");
        assertThat(stat).isPresent();
        assertThat(stat.get().getLockedCount()).isEqualTo(42);
        assertThat(stat.get().getSessionType()).isEqualTo("QUALIFYING");
    }

    @Test
    void onPredictionLocked_duplicateRace_doesNotCreateDuplicateStat() {
        var event = new PredictionLockedEvent("race-002", SessionCompleteEvent.SessionType.QUALIFYING, 10);
        listener.onPredictionLocked(event);
        listener.onPredictionLocked(event);

        assertThat(participationRepo.findAllByOrderByLockedAtDesc()).hasSize(1);
        assertThat(eventRepo.findByRaceIdOrderByOccurredAtAsc("race-002")).hasSize(2);
    }

    @Test
    void getRaceEvents_returnsStoredEvents() throws Exception {
        var event = new PredictionLockedEvent("race-003", SessionCompleteEvent.SessionType.QUALIFYING, 5);
        listener.onPredictionLocked(event);

        mockMvc.perform(get("/analytics/races/race-003/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].eventType").value("PREDICTION_LOCKED"))
            .andExpect(jsonPath("$[0].raceId").value("race-003"));
    }

    @Test
    void getParticipation_returnsStatForRace() throws Exception {
        var event = new PredictionLockedEvent("race-004", SessionCompleteEvent.SessionType.QUALIFYING, 99);
        listener.onPredictionLocked(event);

        mockMvc.perform(get("/analytics/races/race-004/participation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.raceId").value("race-004"))
            .andExpect(jsonPath("$.lockedCount").value(99));
    }

    @Test
    void getParticipation_unknownRace_returns404() throws Exception {
        mockMvc.perform(get("/analytics/races/no-such-race/participation"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getAllParticipation_returnsAllStats() throws Exception {
        listener.onPredictionLocked(new PredictionLockedEvent("race-005", SessionCompleteEvent.SessionType.QUALIFYING, 3));
        listener.onPredictionLocked(new PredictionLockedEvent("race-006", SessionCompleteEvent.SessionType.QUALIFYING, 7));

        mockMvc.perform(get("/analytics/participation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }
}
