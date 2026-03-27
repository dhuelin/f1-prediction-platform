package com.f1predict.scoring.repository;

import com.f1predict.scoring.model.LeagueStanding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeagueStandingRepository extends JpaRepository<LeagueStanding, UUID> {
    Optional<LeagueStanding> findByUserIdAndLeagueId(UUID userId, UUID leagueId);
    List<LeagueStanding> findByLeagueIdOrderByRankAsc(UUID leagueId);

    @Query("SELECT COALESCE(AVG(s.totalPoints), 0) FROM LeagueStanding s WHERE s.leagueId = :leagueId")
    int averagePointsByLeagueId(UUID leagueId);
}
