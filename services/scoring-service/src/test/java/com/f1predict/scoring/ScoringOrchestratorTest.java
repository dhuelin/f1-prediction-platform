package com.f1predict.scoring;

import com.f1predict.common.events.RaceResultFinalEvent;
import com.f1predict.common.events.SessionCompleteEvent;
import com.f1predict.scoring.client.LeagueClient;
import com.f1predict.scoring.client.PredictionClient;
import com.f1predict.scoring.dto.*;
import com.f1predict.scoring.model.RaceScore;
import com.f1predict.scoring.repository.LeagueStandingRepository;
import com.f1predict.scoring.repository.RaceScoreRepository;
import com.f1predict.scoring.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScoringOrchestratorTest {

    @Mock PredictionClient predictionClient;
    @Mock LeagueClient leagueClient;
    @Mock RaceScoreRepository raceScoreRepository;
    @Mock LeagueStandingRepository standingRepository;
    @Mock StandingsService standingsService;

    ScoringOrchestrator orchestrator;

    private final UUID userId = UUID.randomUUID();
    private final UUID leagueId = UUID.randomUUID();

    private static final ScoringConfigData CONFIG = new ScoringConfigData(
        10, 10,
        List.of(new OffsetTier(1, 7), new OffsetTier(2, 2)),
        1, new BigDecimal("2.0"),
        true, true, true, true, true, null
    );

    @BeforeEach
    void setUp() {
        orchestrator = new ScoringOrchestrator(
            predictionClient, leagueClient,
            new ProximityScoreEngine(), new BonusBetScoreEngine(),
            standingsService, raceScoreRepository, standingRepository
        );

        // Default: one league, one member, standard config, no existing score
        when(standingRepository.findDistinctLeagueIds()).thenReturn(List.of(leagueId));
        when(raceScoreRepository.findDistinctLeagueIdsByRaceId(anyString())).thenReturn(List.of());
        when(leagueClient.getMembers(leagueId)).thenReturn(List.of(new LeagueMemberData(userId, 0)));
        when(leagueClient.getConfig(eq(leagueId), anyInt())).thenReturn(CONFIG);
        when(raceScoreRepository.findByUserIdAndLeagueIdAndRaceIdAndSessionType(
                any(), any(), any(), any())).thenReturn(Optional.empty());
        when(raceScoreRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void partialDistance_halvesAllPoints() {
        // VER predicted 1st, actually 1st → 10 topN pts; no bets
        when(predictionClient.getPredictions("2026-01", "RACE")).thenReturn(List.of(
            new PredictionData(userId, "RACE", List.of("VER"), List.of())
        ));

        orchestrator.scoreRace("2026-01", SessionCompleteEvent.SessionType.RACE,
            List.of(new RaceResultFinalEvent.DriverResult("VER", 1, RaceResultFinalEvent.DriverStatus.CLASSIFIED)),
            null, 0, true, false, 1);

        ArgumentCaptor<List<RaceScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(raceScoreRepository).saveAll(captor.capture());
        RaceScore saved = captor.getValue().get(0);

        assertThat(saved.getTopNPoints()).isEqualTo(5);   // 10 / 2
        assertThat(saved.getBonusPoints()).isEqualTo(0);
        assertThat(saved.getTotalPoints()).isEqualTo(5);
        assertThat(saved.isPartialDistance()).isTrue();
    }

    @Test
    void partialDistance_halvesNegativeBonusFloorTowardZero() {
        // Losing FASTEST_LAP bet: stake=50, loss=-50; halved = -25
        when(predictionClient.getPredictions("2026-01", "RACE")).thenReturn(List.of(
            new PredictionData(userId, "RACE", List.of("VER"),
                List.of(new BetData("FASTEST_LAP", 50, "HAM")))  // HAM wrong, VER is fastest
        ));

        orchestrator.scoreRace("2026-01", SessionCompleteEvent.SessionType.RACE,
            List.of(new RaceResultFinalEvent.DriverResult("VER", 1, RaceResultFinalEvent.DriverStatus.CLASSIFIED)),
            "VER", 0, true, false, 1);

        ArgumentCaptor<List<RaceScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(raceScoreRepository).saveAll(captor.capture());
        RaceScore saved = captor.getValue().get(0);

        assertThat(saved.getTopNPoints()).isEqualTo(5);    // 10 / 2
        assertThat(saved.getBonusPoints()).isEqualTo(-25); // -50 / 2
        assertThat(saved.getTotalPoints()).isEqualTo(-20);
    }

    @Test
    void cancelledRace_storesZeroPointsForAllUsers() {
        when(predictionClient.getPredictions("2026-02", "RACE")).thenReturn(List.of(
            new PredictionData(userId, "RACE", List.of("VER"), List.of())
        ));

        orchestrator.scoreRace("2026-02", SessionCompleteEvent.SessionType.RACE,
            List.of(), null, 0, false, true, 1);

        ArgumentCaptor<List<RaceScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(raceScoreRepository).saveAll(captor.capture());
        RaceScore saved = captor.getValue().get(0);

        assertThat(saved.getTopNPoints()).isEqualTo(0);
        assertThat(saved.getBonusPoints()).isEqualTo(0);
        assertThat(saved.getTotalPoints()).isEqualTo(0);
        assertThat(saved.isCancelled()).isTrue();
    }

    @Test
    void fastestLapBet_correctDriver_wins() {
        when(predictionClient.getPredictions("2026-03", "RACE")).thenReturn(List.of(
            new PredictionData(userId, "RACE", List.of(),
                List.of(new BetData("FASTEST_LAP", 50, "VER")))
        ));

        orchestrator.scoreRace("2026-03", SessionCompleteEvent.SessionType.RACE,
            List.of(new RaceResultFinalEvent.DriverResult("VER", 1, RaceResultFinalEvent.DriverStatus.CLASSIFIED)),
            "VER", 0, false, false, 1);

        ArgumentCaptor<List<RaceScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(raceScoreRepository).saveAll(captor.capture());
        RaceScore saved = captor.getValue().get(0);

        assertThat(saved.getBonusPoints()).isEqualTo(100); // 50 * 2.0
    }

    @Test
    void safetyCar_correctCount_wins() {
        when(predictionClient.getPredictions("2026-04", "RACE")).thenReturn(List.of(
            new PredictionData(userId, "RACE", List.of(),
                List.of(new BetData("SC_COUNT", 25, "2")))
        ));

        orchestrator.scoreRace("2026-04", SessionCompleteEvent.SessionType.RACE,
            List.of(), null, 2, false, false, 1);

        ArgumentCaptor<List<RaceScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(raceScoreRepository).saveAll(captor.capture());
        RaceScore saved = captor.getValue().get(0);

        assertThat(saved.getBonusPoints()).isEqualTo(50); // 25 * 2.0
    }

    @Test
    void noDistinctLeagues_skipsScoring() {
        when(standingRepository.findDistinctLeagueIds()).thenReturn(List.of());
        when(raceScoreRepository.findDistinctLeagueIdsByRaceId(anyString())).thenReturn(List.of());
        // predictions are fetched before league discovery is checked, but scoreLeague is never called
        when(predictionClient.getPredictions(anyString(), anyString())).thenReturn(
            List.of(new PredictionData(userId, "RACE", List.of("VER"), List.of()))
        );

        orchestrator.scoreRace("2026-05", SessionCompleteEvent.SessionType.RACE,
            List.of(), null, 0, false, false, 1);

        verify(leagueClient, never()).getMembers(any());
        verify(raceScoreRepository, never()).saveAll(anyList());
    }
}
