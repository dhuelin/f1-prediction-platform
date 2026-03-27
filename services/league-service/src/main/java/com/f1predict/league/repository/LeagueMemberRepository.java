package com.f1predict.league.repository;

import com.f1predict.league.model.LeagueMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeagueMemberRepository extends JpaRepository<LeagueMember, UUID> {
    Optional<LeagueMember> findByLeagueIdAndUserId(UUID leagueId, UUID userId);
    List<LeagueMember> findByLeagueId(UUID leagueId);
    List<LeagueMember> findByUserId(UUID userId);
    long countByLeagueId(UUID leagueId);
}
