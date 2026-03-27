package com.f1predict.scoring.service;

import com.f1predict.common.events.SessionCompleteEvent;
import com.f1predict.scoring.model.LeagueStanding;
import com.f1predict.scoring.publisher.ScoringEventPublisher;
import com.f1predict.scoring.repository.LeagueStandingRepository;
import com.f1predict.scoring.repository.RaceScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StandingsService {

    private final RaceScoreRepository raceScoreRepository;
    private final LeagueStandingRepository standingRepository;
    private final ScoringEventPublisher eventPublisher;

    public StandingsService(RaceScoreRepository raceScoreRepository,
                            LeagueStandingRepository standingRepository,
                            ScoringEventPublisher eventPublisher) {
        this.raceScoreRepository = raceScoreRepository;
        this.standingRepository = standingRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void recalculate(UUID leagueId, String raceId, SessionCompleteEvent.SessionType sessionType) {
        // Collect all unique users who have ever scored in this league
        Set<UUID> userIds = raceScoreRepository.findByLeagueId(leagueId)
            .stream()
            .map(rs -> rs.getUserId())
            .collect(Collectors.toSet());

        // Upsert standings for each user
        List<LeagueStanding> standings = new ArrayList<>();
        for (UUID userId : userIds) {
            int total = raceScoreRepository.sumPointsByUserIdAndLeagueId(userId, leagueId);
            LeagueStanding standing = standingRepository
                .findByUserIdAndLeagueId(userId, leagueId)
                .orElse(new LeagueStanding());
            standing.setUserId(userId);
            standing.setLeagueId(leagueId);
            standing.setTotalPoints(total);
            standing.setUpdatedAt(Instant.now());
            standings.add(standing);
        }

        // Assign ranks (ties share the same rank)
        standings.sort(Comparator.comparingInt(LeagueStanding::getTotalPoints).reversed());
        int rank = 1;
        for (int i = 0; i < standings.size(); i++) {
            if (i > 0 && standings.get(i).getTotalPoints() < standings.get(i - 1).getTotalPoints()) {
                rank = i + 1;
            }
            standings.get(i).setRank(rank);
        }

        standingRepository.saveAll(standings);

        eventPublisher.publishStandingsUpdated(raceId, sessionType, leagueId, standings.size());
    }
}
