package com.f1predict.scoring.service;

import com.f1predict.scoring.client.F1DataClient;
import com.f1predict.scoring.client.LeagueClient;
import com.f1predict.scoring.client.PredictionClient;
import com.f1predict.scoring.dto.*;
import com.f1predict.scoring.repository.RaceScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectedScoreService {

    private static final Logger log = LoggerFactory.getLogger(ProjectedScoreService.class);

    private final F1DataClient f1DataClient;
    private final PredictionClient predictionClient;
    private final LeagueClient leagueClient;
    private final ProximityScoreEngine proximityEngine;
    private final RaceScoreRepository raceScoreRepository;

    public ProjectedScoreService(F1DataClient f1DataClient,
                                  PredictionClient predictionClient,
                                  LeagueClient leagueClient,
                                  ProximityScoreEngine proximityEngine,
                                  RaceScoreRepository raceScoreRepository) {
        this.f1DataClient = f1DataClient;
        this.predictionClient = predictionClient;
        this.leagueClient = leagueClient;
        this.proximityEngine = proximityEngine;
        this.raceScoreRepository = raceScoreRepository;
    }

    /**
     * Computes projected race points for each league member based on current live positions.
     * Bonus bets are excluded — live bet state is unavailable mid-race.
     * Results are not persisted; they are ephemeral snapshots.
     */
    public List<ProjectedStandingEntry> computeProjected(String raceId, UUID leagueId, int raceNumber) {
        List<LivePositionData> livePositions = f1DataClient.getLivePositions();
        if (livePositions.isEmpty()) {
            log.debug("No live positions available — projected score unavailable for race {}", raceId);
            return List.of();
        }

        // Convert to DriverResult list ordered by current race position
        List<DriverResult> currentOrder = livePositions.stream()
                .sorted((a, b) -> Integer.compare(a.position(), b.position()))
                .map(p -> new DriverResult(p.driverCode()))
                .toList();

        List<PredictionData> predictions = predictionClient.getPredictions(raceId, "RACE");
        if (predictions.isEmpty()) {
            return List.of();
        }

        List<LeagueMemberData> members = leagueClient.getMembers(leagueId);
        ScoringConfigData config = leagueClient.getConfig(leagueId, raceNumber);

        return members.stream().map(member -> {
            PredictionData prediction = predictions.stream()
                    .filter(p -> p.userId().equals(member.userId()))
                    .findFirst()
                    .orElse(null);

            int projectedPoints = 0;
            if (prediction != null) {
                projectedPoints = proximityEngine.scoreTopN(
                        prediction.rankedDriverCodes(), currentOrder, config);
            }

            int currentPoints = raceScoreRepository.sumPointsByUserIdAndLeagueId(member.userId(), leagueId);
            return new ProjectedStandingEntry(member.userId(), projectedPoints, currentPoints);
        }).toList();
    }
}
