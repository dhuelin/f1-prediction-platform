package com.f1predict.scoring.service;

import com.f1predict.common.events.RaceResultFinalEvent;
import com.f1predict.common.events.SessionCompleteEvent;
import com.f1predict.scoring.client.LeagueClient;
import com.f1predict.scoring.client.PredictionClient;
import com.f1predict.scoring.dto.*;
import com.f1predict.scoring.model.RaceScore;
import com.f1predict.scoring.repository.LeagueStandingRepository;
import com.f1predict.scoring.repository.RaceScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScoringOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ScoringOrchestrator.class);

    private final PredictionClient predictionClient;
    private final LeagueClient leagueClient;
    private final ProximityScoreEngine proximityEngine;
    private final BonusBetScoreEngine bonusBetEngine;
    private final StandingsService standingsService;
    private final RaceScoreRepository raceScoreRepository;
    private final LeagueStandingRepository standingRepository;

    public ScoringOrchestrator(PredictionClient predictionClient,
                                LeagueClient leagueClient,
                                ProximityScoreEngine proximityEngine,
                                BonusBetScoreEngine bonusBetEngine,
                                StandingsService standingsService,
                                RaceScoreRepository raceScoreRepository,
                                LeagueStandingRepository standingRepository) {
        this.predictionClient = predictionClient;
        this.leagueClient = leagueClient;
        this.proximityEngine = proximityEngine;
        this.bonusBetEngine = bonusBetEngine;
        this.standingsService = standingsService;
        this.raceScoreRepository = raceScoreRepository;
        this.standingRepository = standingRepository;
    }

    /**
     * Called on RACE_RESULT_FINAL and RESULT_AMENDED events.
     * Fetches all league IDs that have standings or prior scores for this race,
     * scores each user per league, persists race_scores, then recalculates standings.
     */
    public void scoreRace(String raceId,
                          SessionCompleteEvent.SessionType sessionType,
                          List<RaceResultFinalEvent.DriverResult> rawResults,
                          String fastestLapDriver,
                          int safetyCarsDeployed,
                          boolean partialDistance,
                          boolean cancelled,
                          int raceNumber) {
        String sessionTypeStr = sessionType.name();

        RaceResultData result = buildResultData(rawResults, fastestLapDriver, safetyCarsDeployed,
                partialDistance, cancelled);

        List<PredictionData> predictions = predictionClient.getPredictions(raceId, sessionTypeStr);
        if (predictions.isEmpty()) {
            log.info("No predictions found for race {} {} — nothing to score", raceId, sessionTypeStr);
            return;
        }

        // Collect distinct league IDs from: (a) all known standings, (b) prior scores for this race
        Set<UUID> leagueIds = new LinkedHashSet<>(standingRepository.findDistinctLeagueIds());
        leagueIds.addAll(raceScoreRepository.findDistinctLeagueIdsByRaceId(raceId));

        for (UUID leagueId : leagueIds) {
            scoreLeague(leagueId, raceId, sessionType, sessionTypeStr,
                predictions, result, partialDistance, cancelled, raceNumber);
        }
    }

    @Transactional
    public void scoreLeague(UUID leagueId,
                             String raceId,
                             SessionCompleteEvent.SessionType sessionType,
                             String sessionTypeStr,
                             List<PredictionData> predictions,
                             RaceResultData result,
                             boolean partialDistance,
                             boolean cancelled,
                             int raceNumber) {
        List<LeagueMemberData> members = leagueClient.getMembers(leagueId);
        if (members.isEmpty()) return;

        ScoringConfigData config = leagueClient.getConfig(leagueId, raceNumber);

        List<RaceScore> scores = new ArrayList<>();
        for (PredictionData prediction : predictions) {
            boolean isMember = members.stream()
                .anyMatch(m -> m.userId().equals(prediction.userId()));
            if (!isMember) continue;

            RaceScore score = raceScoreRepository
                .findByUserIdAndLeagueIdAndRaceIdAndSessionType(
                    prediction.userId(), leagueId, raceId, sessionTypeStr)
                .orElse(new RaceScore());

            score.setUserId(prediction.userId());
            score.setLeagueId(leagueId);
            score.setRaceId(raceId);
            score.setSessionType(sessionTypeStr);
            score.setPartialDistance(partialDistance);
            score.setCancelled(cancelled);
            score.setScoredAt(Instant.now());

            if (cancelled) {
                score.setTopNPoints(0);
                score.setBonusPoints(0);
                score.setTotalPoints(0);
            } else {
                int topN = proximityEngine.scoreTopN(
                    prediction.rankedDriverCodes(),
                    result.classifiedFinishers(),
                    config);

                int bonus = bonusBetEngine.scoreBets(
                    prediction.bets(),
                    result,
                    config);

                // Partial distance: halve all points (integer floor toward zero)
                if (partialDistance) {
                    topN = topN / 2;
                    bonus = bonus / 2;
                }

                score.setTopNPoints(topN);
                score.setBonusPoints(bonus);
                score.setTotalPoints(topN + bonus);
            }

            scores.add(score);
        }

        raceScoreRepository.saveAll(scores);
        standingsService.recalculate(leagueId, raceId, sessionType);
    }

    private RaceResultData buildResultData(List<RaceResultFinalEvent.DriverResult> raw,
                                            String fastestLapDriver,
                                            int safetyCarsDeployed,
                                            boolean partialDistance,
                                            boolean cancelled) {
        if (raw == null || raw.isEmpty()) {
            return new RaceResultData(List.of(), fastestLapDriver, List.of(),
                    safetyCarsDeployed, partialDistance, cancelled);
        }

        List<DriverResult> classified = raw.stream()
            .filter(r -> r.status() == RaceResultFinalEvent.DriverStatus.CLASSIFIED)
            .sorted(java.util.Comparator.comparingInt(RaceResultFinalEvent.DriverResult::finishPosition))
            .map(r -> new DriverResult(r.driverCode()))
            .collect(Collectors.toList());

        List<String> dnfDsqDns = raw.stream()
            .filter(r -> r.status() == RaceResultFinalEvent.DriverStatus.DNF
                      || r.status() == RaceResultFinalEvent.DriverStatus.DSQ
                      || r.status() == RaceResultFinalEvent.DriverStatus.DNS)
            .map(RaceResultFinalEvent.DriverResult::driverCode)
            .collect(Collectors.toList());

        return new RaceResultData(classified, fastestLapDriver, dnfDsqDns,
                safetyCarsDeployed, partialDistance, cancelled);
    }
}
