package com.f1predict.scoring.repository;

import com.f1predict.scoring.model.RaceScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RaceScoreRepository extends JpaRepository<RaceScore, UUID> {
    Optional<RaceScore> findByUserIdAndLeagueIdAndRaceIdAndSessionType(UUID userId, UUID leagueId, String raceId, String sessionType);
    List<RaceScore> findByLeagueId(UUID leagueId);
    List<RaceScore> findByUserIdAndLeagueId(UUID userId, UUID leagueId);

    @Query("SELECT COALESCE(SUM(r.totalPoints), 0) FROM RaceScore r WHERE r.userId = :userId AND r.leagueId = :leagueId")
    int sumPointsByUserIdAndLeagueId(UUID userId, UUID leagueId);

    @Query("SELECT COALESCE(AVG(sub.total), 0) FROM (SELECT SUM(r.totalPoints) AS total FROM RaceScore r WHERE r.leagueId = :leagueId GROUP BY r.userId) sub")
    int averagePointsByLeagueId(UUID leagueId);

    @Query("SELECT DISTINCT rs.leagueId FROM RaceScore rs WHERE rs.raceId = :raceId")
    List<UUID> findDistinctLeagueIdsByRaceId(String raceId);
}
