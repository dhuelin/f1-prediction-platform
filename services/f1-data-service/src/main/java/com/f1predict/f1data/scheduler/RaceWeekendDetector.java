package com.f1predict.f1data.scheduler;

import com.f1predict.f1data.model.Session;
import com.f1predict.f1data.repository.SessionRepository;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class RaceWeekendDetector {
    private static final long PRE_SESSION_BUFFER_MINUTES = 30;
    private static final long POST_SESSION_BUFFER_HOURS = 3;

    private final SessionRepository sessionRepository;

    public RaceWeekendDetector(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /** True if any session for the current round is within the next 5 days */
    public boolean isRaceWeekend() {
        Instant now = Instant.now();
        return sessionRepository.existsByScheduledAtBetweenAndCompletedFalse(
            now, now.plus(5, ChronoUnit.DAYS));
    }

    /** True if a session is currently in progress (within time window) */
    public boolean isSessionActive() {
        return isQualifyingActive() || isRaceActive() || isSprintActive();
    }

    /** True if a QUALIFYING or SPRINT_SHOOTOUT session is currently active */
    public boolean isQualifyingActive() {
        return isSessionTypeActive(Session.SessionType.QUALIFYING) ||
               isSessionTypeActive(Session.SessionType.SPRINT_SHOOTOUT);
    }

    /** True if a RACE session is currently active */
    public boolean isRaceActive() {
        return isSessionTypeActive(Session.SessionType.RACE);
    }

    public boolean isSprintActive() {
        return isSessionTypeActive(Session.SessionType.SPRINT);
    }

    private boolean isSessionTypeActive(Session.SessionType type) {
        Instant now = Instant.now();
        Instant windowStart = now.minus(POST_SESSION_BUFFER_HOURS, ChronoUnit.HOURS);
        Instant windowEnd = now.plus(PRE_SESSION_BUFFER_MINUTES, ChronoUnit.MINUTES);
        return sessionRepository.existsBySessionTypeAndScheduledAtBetweenAndCompletedFalse(
            type, windowStart, windowEnd);
    }
}
