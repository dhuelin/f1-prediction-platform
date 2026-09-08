package com.f1predict.league.repository;

import com.f1predict.league.model.ScoringConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScoringConfigRepository extends JpaRepository<ScoringConfig, UUID> {
    List<ScoringConfig> findByLeagueIdOrderByEffectiveFromRaceDesc(UUID leagueId);
    Optional<ScoringConfig> findTopByLeagueIdOrderByEffectiveFromRaceDesc(UUID leagueId);

    /** Newest config in force at or before a given race number. */
    Optional<ScoringConfig> findTopByLeagueIdAndEffectiveFromRaceLessThanEqualOrderByEffectiveFromRaceDesc(
        UUID leagueId, int raceNumber);

    /** Earliest config for a league — fallback when every config starts after the race in question. */
    Optional<ScoringConfig> findTopByLeagueIdOrderByEffectiveFromRaceAsc(UUID leagueId);
}
