package com.f1predict.f1data.service;

import com.f1predict.f1data.client.JolpicaClient;
import com.f1predict.f1data.dto.jolpica.JolpicaRaceDto;
import com.f1predict.f1data.model.Session;
import com.f1predict.f1data.repository.RaceRepository;
import com.f1predict.f1data.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class RaceCalendarServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    JolpicaClient jolpicaClient;

    @Autowired
    RaceCalendarService raceCalendarService;

    @Autowired
    RaceRepository raceRepository;

    @Autowired
    SessionRepository sessionRepository;

    @BeforeEach
    void clearRepositories() {
        sessionRepository.deleteAll();
        raceRepository.deleteAll();
    }

    @Test
    void syncCalendar_persistsRace() {
        var circuit = new JolpicaRaceDto.Circuit(
            "Bahrain International Circuit",
            new JolpicaRaceDto.Circuit.Location("Bahrain")
        );
        var raceDto = new JolpicaRaceDto(
            "1",
            "Bahrain Grand Prix",
            circuit,
            "2025-03-16",
            "15:00:00Z",
            null
        );
        when(jolpicaClient.fetchCalendar(2025)).thenReturn(List.of(raceDto));

        raceCalendarService.syncCalendar(2025);

        var races = raceRepository.findBySeason(2025);
        assertThat(races).hasSize(1);
        var race = races.get(0);
        assertThat(race.getRaceName()).isEqualTo("Bahrain Grand Prix");
        assertThat(race.getCircuitName()).isEqualTo("Bahrain International Circuit");
        assertThat(race.getCountry()).isEqualTo("Bahrain");
        assertThat(race.getRound()).isEqualTo(1);
        assertThat(race.getSeason()).isEqualTo(2025);
        assertThat(race.isSprintWeekend()).isFalse();

        var sessions = sessionRepository.findByRaceId(race.getId());
        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getSessionType()).isEqualTo(Session.SessionType.RACE);
        assertThat(sessions.get(0).getScheduledAt()).isEqualTo(Instant.parse("2025-03-16T15:00:00Z"));
    }

    @Test
    void syncCalendar_persistsSprintSessions() {
        var circuit = new JolpicaRaceDto.Circuit(
            "Miami International Autodrome",
            new JolpicaRaceDto.Circuit.Location("USA")
        );
        var sprintInfo = new JolpicaRaceDto.SprintInfo("2025-05-03", "19:00:00Z");
        var raceDto = new JolpicaRaceDto(
            "5",
            "Miami Grand Prix",
            circuit,
            "2025-05-04",
            "19:00:00Z",
            sprintInfo
        );
        when(jolpicaClient.fetchCalendar(2025)).thenReturn(List.of(raceDto));

        raceCalendarService.syncCalendar(2025);

        var races = raceRepository.findBySeason(2025);
        assertThat(races).hasSize(1);
        var race = races.get(0);
        assertThat(race.isSprintWeekend()).isTrue();

        var sessions = sessionRepository.findByRaceId(race.getId());
        assertThat(sessions).hasSize(2);
        var sessionTypes = sessions.stream().map(Session::getSessionType).toList();
        assertThat(sessionTypes).containsExactlyInAnyOrder(
            Session.SessionType.RACE,
            Session.SessionType.SPRINT
        );
    }

    @Test
    void syncCalendar_isIdempotent() {
        var circuit = new JolpicaRaceDto.Circuit(
            "Bahrain International Circuit",
            new JolpicaRaceDto.Circuit.Location("Bahrain")
        );
        var raceDto = new JolpicaRaceDto(
            "1",
            "Bahrain Grand Prix",
            circuit,
            "2025-03-16",
            "15:00:00Z",
            null
        );
        when(jolpicaClient.fetchCalendar(2025)).thenReturn(List.of(raceDto));

        raceCalendarService.syncCalendar(2025);
        raceCalendarService.syncCalendar(2025);

        var races = raceRepository.findBySeason(2025);
        assertThat(races).hasSize(1);

        var sessions = sessionRepository.findByRaceId(races.get(0).getId());
        assertThat(sessions).hasSize(1);
    }
}
