package com.f1predict.scoring.controller;

import com.f1predict.scoring.dto.StandingEntry;
import com.f1predict.scoring.model.LeagueStanding;
import com.f1predict.scoring.repository.LeagueStandingRepository;
import com.f1predict.scoring.repository.RaceScoreRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/scores")
public class ScoringController {

    private final LeagueStandingRepository standingRepository;
    private final RaceScoreRepository raceScoreRepository;

    public ScoringController(LeagueStandingRepository standingRepository,
                              RaceScoreRepository raceScoreRepository) {
        this.standingRepository = standingRepository;
        this.raceScoreRepository = raceScoreRepository;
    }

    /** Called by Prediction Service for stake validation */
    @GetMapping("/balance/{userId}/leagues/{leagueId}")
    public int getBalance(@PathVariable UUID userId, @PathVariable UUID leagueId) {
        return raceScoreRepository.sumPointsByUserIdAndLeagueId(userId, leagueId);
    }

    /** Called by League Service for mid-season catch-up */
    @GetMapping("/leagues/{leagueId}/average-points")
    public int getAveragePoints(@PathVariable UUID leagueId) {
        return standingRepository.averagePointsByLeagueId(leagueId);
    }

    /** Public standings endpoint */
    @GetMapping("/leagues/{leagueId}/standings")
    public List<StandingEntry> getStandings(@PathVariable UUID leagueId) {
        return standingRepository.findByLeagueIdOrderByRankAsc(leagueId)
            .stream()
            .map(s -> new StandingEntry(s.getUserId(), s.getTotalPoints(), s.getRank()))
            .toList();
    }
}
