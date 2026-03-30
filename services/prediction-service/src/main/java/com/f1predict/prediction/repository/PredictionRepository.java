package com.f1predict.prediction.repository;

import com.f1predict.prediction.model.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PredictionRepository extends JpaRepository<Prediction, UUID> {
    Optional<Prediction> findByUserIdAndRaceIdAndSessionType(UUID userId, String raceId, String sessionType);
    List<Prediction> findByRaceIdAndSessionType(String raceId, String sessionType);
    List<Prediction> findByRaceId(String raceId);
    List<Prediction> findByRaceIdAndSessionTypeAndLockedTrue(String raceId, String sessionType);
}
