package com.f1predict.league.repository;

import com.f1predict.league.model.ScoringConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScoringConfigRepository extends JpaRepository<ScoringConfig, UUID> {
    List<ScoringConfig> findByLeagueIdOrderByEffectiveFromRaceDesc(UUID leagueId);
    Optional<ScoringConfig> findTopByLeagueIdOrderByEffectiveFromRaceDesc(UUID leagueId);
}
