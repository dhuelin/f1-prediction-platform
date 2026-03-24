package com.f1predict.f1data.service;

import com.f1predict.common.events.RaceResultFinalEvent;
import com.f1predict.common.events.ResultAmendedEvent;
import com.f1predict.common.events.SessionCompleteEvent;
import com.f1predict.f1data.client.JolpicaClient;
import com.f1predict.f1data.dto.jolpica.JolpicaResultDto;
import com.f1predict.f1data.model.Race;
import com.f1predict.f1data.model.RaceResult;
import com.f1predict.f1data.model.Session;
import com.f1predict.f1data.publisher.F1EventPublisher;
import com.f1predict.f1data.repository.RaceRepository;
import com.f1predict.f1data.repository.RaceResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultAmendmentServiceTest {

    @Mock
    private JolpicaClient jolpicaClient;

    @Mock
    private RaceRepository raceRepository;

    @Mock
    private RaceResultRepository raceResultRepository;

    @Mock
    private F1EventPublisher eventPublisher;

    @InjectMocks
    private ResultAmendmentService resultAmendmentService;

    @Test
    void checkForAmendments_publishesAmendedEvent_whenPositionChanged() {
        // Arrange: a race completed within the last 48h
        Race race = new Race();
        race.setId("2025-01");
        race.setSeason(2025);
        race.setRound(1);
        race.setRaceName("Bahrain Grand Prix");
        race.setCircuitName("Bahrain International Circuit");
        race.setCountry("Bahrain");
        race.setRaceDate(Instant.now().minusSeconds(3600)); // 1 hour ago

        when(raceRepository.findByRaceDateBetween(any(Instant.class), any(Instant.class)))
            .thenReturn(List.of(race));

        // Stored result: VER in P1, CLASSIFIED
        RaceResult storedResult = new RaceResult();
        storedResult.setDriverCode("VER");
        storedResult.setFinishPosition(1);
        storedResult.setStatus(RaceResult.DriverStatus.CLASSIFIED);
        storedResult.setSessionType(Session.SessionType.RACE);

        when(raceResultRepository.findByRaceIdAndSessionType("2025-01", Session.SessionType.RACE))
            .thenReturn(List.of(storedResult));

        // Jolpica returns VER in P3 (time penalty applied)
        JolpicaResultDto verDto = new JolpicaResultDto(
            "3",
            new JolpicaResultDto.Driver("VER"),
            "Finished",
            null
        );
        when(jolpicaClient.fetchRaceResults(2025, 1)).thenReturn(List.of(verDto));

        // Act
        resultAmendmentService.checkForAmendments();

        // Assert: publishResultAmended called once with correct payload
        ArgumentCaptor<ResultAmendedEvent> captor = ArgumentCaptor.forClass(ResultAmendedEvent.class);
        verify(eventPublisher, times(1)).publishResultAmended(captor.capture());

        ResultAmendedEvent published = captor.getValue();
        assertThat(published.raceId()).isEqualTo("2025-01");
        assertThat(published.sessionType()).isEqualTo(SessionCompleteEvent.SessionType.RACE);
        assertThat(published.amendmentReason()).isEqualTo("POST_RACE_TIME_PENALTY");
        assertThat(published.amendedResults()).hasSize(1);

        RaceResultFinalEvent.DriverResult driverResult = published.amendedResults().get(0);
        assertThat(driverResult.driverCode()).isEqualTo("VER");
        assertThat(driverResult.finishPosition()).isEqualTo(3);
        assertThat(driverResult.status()).isEqualTo(RaceResultFinalEvent.DriverStatus.CLASSIFIED);
    }

    @Test
    void checkForAmendments_publishesDsqEvent_whenStatusBecomesDisqualified() {
        Race race = new Race();
        race.setId("2025-01");
        race.setSeason(2025);
        race.setRound(1);
        race.setRaceName("Bahrain Grand Prix");
        race.setCircuitName("Bahrain International Circuit");
        race.setCountry("Bahrain");
        race.setRaceDate(Instant.now().minusSeconds(3600));

        when(raceRepository.findByRaceDateBetween(any(Instant.class), any(Instant.class)))
            .thenReturn(List.of(race));

        RaceResult storedResult = new RaceResult();
        storedResult.setDriverCode("VER");
        storedResult.setFinishPosition(1);
        storedResult.setStatus(RaceResult.DriverStatus.CLASSIFIED);
        storedResult.setSessionType(Session.SessionType.RACE);

        when(raceResultRepository.findByRaceIdAndSessionType("2025-01", Session.SessionType.RACE))
            .thenReturn(List.of(storedResult));

        JolpicaResultDto verDto = new JolpicaResultDto(
            "1",
            new JolpicaResultDto.Driver("VER"),
            "Disqualified",
            null
        );
        when(jolpicaClient.fetchRaceResults(2025, 1)).thenReturn(List.of(verDto));

        resultAmendmentService.checkForAmendments();

        ArgumentCaptor<ResultAmendedEvent> captor = ArgumentCaptor.forClass(ResultAmendedEvent.class);
        verify(eventPublisher, times(1)).publishResultAmended(captor.capture());

        ResultAmendedEvent published = captor.getValue();
        assertThat(published.amendmentReason()).isEqualTo("POST_RACE_DSQ");
        assertThat(published.amendedResults().get(0).status())
            .isEqualTo(RaceResultFinalEvent.DriverStatus.DSQ);
    }

    @Test
    void checkForAmendments_doesNotPublish_whenNothingChanged() {
        Race race = new Race();
        race.setId("2025-01");
        race.setSeason(2025);
        race.setRound(1);
        race.setRaceName("Bahrain Grand Prix");
        race.setCircuitName("Bahrain International Circuit");
        race.setCountry("Bahrain");
        race.setRaceDate(Instant.now().minusSeconds(3600));

        when(raceRepository.findByRaceDateBetween(any(Instant.class), any(Instant.class)))
            .thenReturn(List.of(race));

        RaceResult storedResult = new RaceResult();
        storedResult.setDriverCode("VER");
        storedResult.setFinishPosition(1);
        storedResult.setStatus(RaceResult.DriverStatus.CLASSIFIED);
        storedResult.setSessionType(Session.SessionType.RACE);

        when(raceResultRepository.findByRaceIdAndSessionType("2025-01", Session.SessionType.RACE))
            .thenReturn(List.of(storedResult));

        // Same position and status as stored
        JolpicaResultDto verDto = new JolpicaResultDto(
            "1",
            new JolpicaResultDto.Driver("VER"),
            "Finished",
            null
        );
        when(jolpicaClient.fetchRaceResults(2025, 1)).thenReturn(List.of(verDto));

        resultAmendmentService.checkForAmendments();

        verify(eventPublisher, never()).publishResultAmended(any());
    }

    @Test
    void checkForAmendments_continuesProcessing_whenJolpicaThrows() {
        Race race1 = new Race();
        race1.setId("2025-01");
        race1.setSeason(2025);
        race1.setRound(1);
        race1.setRaceName("Bahrain Grand Prix");
        race1.setCircuitName("Bahrain International Circuit");
        race1.setCountry("Bahrain");
        race1.setRaceDate(Instant.now().minusSeconds(3600));

        Race race2 = new Race();
        race2.setId("2025-02");
        race2.setSeason(2025);
        race2.setRound(2);
        race2.setRaceName("Saudi Arabian Grand Prix");
        race2.setCircuitName("Jeddah Corniche Circuit");
        race2.setCountry("Saudi Arabia");
        race2.setRaceDate(Instant.now().minusSeconds(7200));

        when(raceRepository.findByRaceDateBetween(any(Instant.class), any(Instant.class)))
            .thenReturn(List.of(race1, race2));

        // First race throws F1ApiException
        when(jolpicaClient.fetchRaceResults(2025, 1))
            .thenThrow(new com.f1predict.f1data.client.F1ApiException("API error", null));

        RaceResult storedResult2 = new RaceResult();
        storedResult2.setDriverCode("VER");
        storedResult2.setFinishPosition(1);
        storedResult2.setStatus(RaceResult.DriverStatus.CLASSIFIED);
        storedResult2.setSessionType(Session.SessionType.RACE);

        when(raceResultRepository.findByRaceIdAndSessionType("2025-02", Session.SessionType.RACE))
            .thenReturn(List.of(storedResult2));

        JolpicaResultDto verDto = new JolpicaResultDto(
            "3",
            new JolpicaResultDto.Driver("VER"),
            "Finished",
            null
        );
        when(jolpicaClient.fetchRaceResults(2025, 2)).thenReturn(List.of(verDto));

        // Should not throw; second race should still be processed
        resultAmendmentService.checkForAmendments();

        ArgumentCaptor<ResultAmendedEvent> captor = ArgumentCaptor.forClass(ResultAmendedEvent.class);
        verify(eventPublisher, times(1)).publishResultAmended(captor.capture());
        ResultAmendedEvent capturedEvent = captor.getValue();
        assertThat(capturedEvent.raceId()).isEqualTo("2025-02");
    }
}
