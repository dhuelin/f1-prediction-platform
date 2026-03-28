package com.f1predict.scoring;

import com.f1predict.scoring.dto.*;
import com.f1predict.scoring.model.LeagueStanding;
import com.f1predict.scoring.model.RaceScore;
import com.f1predict.scoring.repository.LeagueStandingRepository;
import com.f1predict.scoring.repository.RaceScoreRepository;
import com.f1predict.scoring.service.ScoringOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ScoringIntegrationTest {

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
    @Autowired RaceScoreRepository raceScoreRepository;
    @Autowired LeagueStandingRepository standingRepository;

    // We test the scoring engines directly rather than via the full orchestrator
    // (which requires live Prediction/League services) — unit tests cover the engines.
    // Here we test the REST endpoints and repository layer.

    private final UUID userId1 = UUID.randomUUID();
    private final UUID userId2 = UUID.randomUUID();
    private final UUID leagueId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        standingRepository.deleteAll();
        raceScoreRepository.deleteAll();
    }

    @Test
    void getBalance_noScores_returnsZero() throws Exception {
        mockMvc.perform(get("/scores/balance/" + userId1 + "/leagues/" + leagueId))
            .andExpect(status().isOk())
            .andExpect(content().string("0"));
    }

    @Test
    void getBalance_withScores_returnsSum() throws Exception {
        RaceScore s1 = score(userId1, leagueId, "2026-01", "RACE", 25, 10);
        RaceScore s2 = score(userId1, leagueId, "2026-02", "RACE", 15, 5);
        raceScoreRepository.saveAll(List.of(s1, s2));

        mockMvc.perform(get("/scores/balance/" + userId1 + "/leagues/" + leagueId))
            .andExpect(status().isOk())
            .andExpect(content().string("55")); // 35 + 20
    }

    @Test
    void getStandings_orderedByRank() throws Exception {
        LeagueStanding ls1 = standing(userId1, leagueId, 100, 1);
        LeagueStanding ls2 = standing(userId2, leagueId, 80, 2);
        standingRepository.saveAll(List.of(ls1, ls2));

        mockMvc.perform(get("/scores/leagues/" + leagueId + "/standings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].totalPoints").value(100))
            .andExpect(jsonPath("$[0].rank").value(1))
            .andExpect(jsonPath("$[1].totalPoints").value(80))
            .andExpect(jsonPath("$[1].rank").value(2));
    }

    @Test
    void getAveragePoints_computedFromStandings() throws Exception {
        standingRepository.saveAll(List.of(
            standing(userId1, leagueId, 100, 1),
            standing(userId2, leagueId, 60, 2)
        ));

        mockMvc.perform(get("/scores/leagues/" + leagueId + "/average-points"))
            .andExpect(status().isOk())
            .andExpect(content().string("80"));
    }

    // --- helpers ---

    private RaceScore score(UUID userId, UUID leagueId, String raceId, String session, int topN, int bonus) {
        RaceScore s = new RaceScore();
        s.setUserId(userId);
        s.setLeagueId(leagueId);
        s.setRaceId(raceId);
        s.setSessionType(session);
        s.setTopNPoints(topN);
        s.setBonusPoints(bonus);
        s.setTotalPoints(topN + bonus);
        return s;
    }

    private LeagueStanding standing(UUID userId, UUID leagueId, int points, int rank) {
        LeagueStanding ls = new LeagueStanding();
        ls.setUserId(userId);
        ls.setLeagueId(leagueId);
        ls.setTotalPoints(points);
        ls.setRank(rank);
        return ls;
    }
}
