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
import java.util.List;
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
     * Fetches all league IDs that have members with predictions, scores each user per league,
     * persists race_scores, then recalculates standings.
     */
    public void scoreRace(String raceId,
                          SessionCompleteEvent.SessionType sessionType,
                          List<RaceResultFinalEvent.DriverResult> rawResults,
                          boolean partialDistance,
                          boolean cancelled,
                          int raceNumber) {
        String sessionTypeStr = sessionType.name();

        // Build structured result data
        RaceResultData result = buildResultData(rawResults, partialDistance, cancelled);

        // Fetch all locked predictions for this race
        List<PredictionData> predictions = predictionClient.getPredictions(raceId, sessionTypeStr);
        if (predictions.isEmpty()) {
            log.info("No predictions found for race {} {} — nothing to score", raceId, sessionTypeStr);
            return;
        }

        // Collect unique league IDs for all users who submitted predictions
        // We query league membership per user to find their leagues
        // For simplicity: we score each prediction against each league the user belongs to
        // The league IDs are discovered by calling League Service for each user
        // (In practice, a dedicated internal endpoint would return all leagues + members)

        // Approach: get all league IDs from standings (leagues with at least one standing)
        // and score predictions for each
        List<UUID> leagueIds = standingRepository.findAll()
            .stream()
            .map(s -> s.getLeagueId())
            .distinct()
            .collect(Collectors.toList());

        // Also include leagues from existing race scores for this race (re-score case)
        raceScoreRepository.findByLeagueId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        List<UUID> leaguesFromScores = raceScoreRepository.findAll()
            .stream()
            .map(rs -> rs.getLeagueId())
            .distinct()
            .collect(Collectors.toList());
        for (UUID lid : leaguesFromScores) {
            if (!leagueIds.contains(lid)) leagueIds.add(lid);
        }

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
                // #79 — race cancelled: zero points
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

                // #78 — partial distance: halve all points (integer floor)
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
                                            boolean partialDistance,
                                            boolean cancelled) {
        if (raw == null || raw.isEmpty()) {
            return new RaceResultData(List.of(), null, List.of(), 0, partialDistance, cancelled);
        }

        List<DriverResult> classified = new ArrayList<>();
        List<String> dnfDsqDns = new ArrayList<>();
        String fastestLap = null;

        for (RaceResultFinalEvent.DriverResult r : raw) {
            switch (r.status()) {
                case CLASSIFIED -> classified.add(new DriverResult(r.driverCode()));
                case DNF, DSQ, DNS -> dnfDsqDns.add(r.driverCode());
            }
        }

        // Sort classified by finishPosition
        classified = raw.stream()
            .filter(r -> r.status() == RaceResultFinalEvent.DriverStatus.CLASSIFIED)
            .sorted(java.util.Comparator.comparingInt(RaceResultFinalEvent.DriverResult::finishPosition))
            .map(r -> new DriverResult(r.driverCode()))
            .collect(Collectors.toList());

        return new RaceResultData(classified, fastestLap, dnfDsqDns, 0, partialDistance, cancelled);
    }
}
