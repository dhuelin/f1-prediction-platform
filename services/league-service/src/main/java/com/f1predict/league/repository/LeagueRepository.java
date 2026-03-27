package com.f1predict.league.repository;

import com.f1predict.league.model.League;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeagueRepository extends JpaRepository<League, UUID> {
    Optional<League> findByName(String name);
    Optional<League> findByInviteCode(String inviteCode);
    List<League> findByVisibility(String visibility);
}
