package com.f1predict.scoring.api;

import com.f1predict.scoring.repository.LeagueStandingRepository;
import com.f1predict.scoring.repository.RaceScoreRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ScoringDirectoryService implements ScoringDirectory {

    private final RaceScoreRepository raceScoreRepository;
    private final LeagueStandingRepository standingRepository;

    public ScoringDirectoryService(RaceScoreRepository raceScoreRepository,
                                   LeagueStandingRepository standingRepository) {
        this.raceScoreRepository = raceScoreRepository;
        this.standingRepository = standingRepository;
    }

    @Override
    public int getBalance(UUID userId, UUID leagueId) {
        return raceScoreRepository.sumPointsByUserIdAndLeagueId(userId, leagueId);
    }

    @Override
    public int getLeagueAveragePoints(UUID leagueId) {
        return standingRepository.averagePointsByLeagueId(leagueId);
    }
}
