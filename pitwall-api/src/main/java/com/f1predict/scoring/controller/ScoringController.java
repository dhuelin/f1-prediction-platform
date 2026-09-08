package com.f1predict.scoring.controller;

import com.f1predict.scoring.dto.ProjectedStandingEntry;
import com.f1predict.scoring.dto.StandingEntry;
import com.f1predict.scoring.repository.LeagueStandingRepository;
import com.f1predict.scoring.repository.RaceScoreRepository;
import com.f1predict.scoring.service.ProjectedScoreService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/scores")
public class ScoringController {

    private final LeagueStandingRepository standingRepository;
    private final RaceScoreRepository raceScoreRepository;
    private final ProjectedScoreService projectedScoreService;

    public ScoringController(LeagueStandingRepository standingRepository,
                              RaceScoreRepository raceScoreRepository,
                              ProjectedScoreService projectedScoreService) {
        this.standingRepository = standingRepository;
        this.raceScoreRepository = raceScoreRepository;
        this.projectedScoreService = projectedScoreService;
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

    /**
     * Returns projected race scores based on current live positions.
     * Not persisted — call repeatedly during a live race for real-time updates.
     */
    @GetMapping("/races/{raceId}/projected")
    public List<ProjectedStandingEntry> getProjectedStandings(
            @PathVariable String raceId,
            @RequestParam UUID leagueId,
            @RequestParam int raceNumber) {
        return projectedScoreService.computeProjected(raceId, leagueId, raceNumber);
    }
}
