package com.f1predict.prediction.repository;

import com.f1predict.prediction.model.BonusBet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BonusBetRepository extends JpaRepository<BonusBet, UUID> {
    List<BonusBet> findByPredictionId(UUID predictionId);
}
