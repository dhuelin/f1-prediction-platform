package com.f1predict.f1data.repository;

import com.f1predict.f1data.model.Race;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RaceRepository extends JpaRepository<Race, String> {
    List<Race> findBySeason(int season);
    Optional<Race> findBySeasonAndRound(int season, int round);
}
