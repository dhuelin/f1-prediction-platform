package com.f1predict.analytics.repository;

import com.f1predict.analytics.model.PredictionParticipationStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionParticipationStatRepository extends JpaRepository<PredictionParticipationStat, Long> {
    Optional<PredictionParticipationStat> findByRaceId(String raceId);
    List<PredictionParticipationStat> findAllByOrderByLockedAtDesc();
}
