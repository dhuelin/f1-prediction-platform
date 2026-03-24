package com.f1predict.f1data.repository;

import com.f1predict.f1data.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findByRaceId(String raceId);
    List<Session> findByRaceIdAndSessionType(String raceId, Session.SessionType sessionType);
    boolean existsByScheduledAtBetweenAndCompletedFalse(Instant from, Instant to);
    boolean existsBySessionTypeAndScheduledAtBetweenAndCompletedFalse(
        Session.SessionType type, Instant from, Instant to);
}
