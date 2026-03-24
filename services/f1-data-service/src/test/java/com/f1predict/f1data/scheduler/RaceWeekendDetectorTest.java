package com.f1predict.f1data.scheduler;

import com.f1predict.f1data.model.Session;
import com.f1predict.f1data.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaceWeekendDetectorTest {

    @Mock
    SessionRepository sessionRepository;

    @InjectMocks
    RaceWeekendDetector detector;

    @Test
    void isRaceWeekend_returnsFalse_whenNoSessionsInNext5Days() {
        when(sessionRepository.existsByScheduledAtBetweenAndCompletedFalse(any(), any()))
            .thenReturn(false);

        assertThat(detector.isRaceWeekend()).isFalse();
    }

    @Test
    void isRaceWeekend_returnsTrue_whenSessionScheduledIn2Days() {
        // Capture the from/to arguments to verify the 5-day window
        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);

        when(sessionRepository.existsByScheduledAtBetweenAndCompletedFalse(
            fromCaptor.capture(), toCaptor.capture()))
            .thenReturn(true);

        boolean result = detector.isRaceWeekend();

        assertThat(result).isTrue();
        // Verify the to value is approximately 5 days after from
        Instant from = fromCaptor.getValue();
        Instant to = toCaptor.getValue();
        long minutesBetween = ChronoUnit.MINUTES.between(from, to);
        // 5 days = 7200 minutes, allow ±5 minutes for test execution timing
        assertThat(minutesBetween).isBetween(7195L, 7205L);
    }

    @Test
    void isRaceActive_returnsTrue_whenRaceSessionStartsIn20Minutes() {
        // A RACE session scheduled 20 minutes from now should fall within the
        // corrected window of [now-3h, now+30min], so isRaceActive() should return true
        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);

        when(sessionRepository.existsBySessionTypeAndScheduledAtBetweenAndCompletedFalse(
            eq(Session.SessionType.RACE),
            fromCaptor.capture(),
            toCaptor.capture()))
            .thenReturn(true);

        boolean result = detector.isRaceActive();

        assertThat(result).isTrue();
        // Verify the window spans from now-3h to now+30min (total = 210 minutes)
        Instant from = fromCaptor.getValue();
        Instant to = toCaptor.getValue();
        long minutesBetween = ChronoUnit.MINUTES.between(from, to);
        // window = 180 min + 30 min = 210 minutes, allow ±5 for test execution timing
        assertThat(minutesBetween).isBetween(205L, 215L);
        // Verify "from" is approximately 3 hours (180 minutes) before now
        long minutesBeforeNow = ChronoUnit.MINUTES.between(from, Instant.now());
        assertThat(minutesBeforeNow).isBetween(175L, 185L);
    }

    @Test
    void isRaceActive_returnsFalse_whenRaceSessionEnded4HoursAgo() {
        // A RACE session that ended 4 hours ago — scheduledAt = now-4h — falls outside
        // the corrected window [now-3h, now+30min], so isRaceActive() should return false
        when(sessionRepository.existsBySessionTypeAndScheduledAtBetweenAndCompletedFalse(
            eq(Session.SessionType.RACE), any(), any()))
            .thenReturn(false);

        assertThat(detector.isRaceActive()).isFalse();
    }

    @Test
    void isQualifyingActive_returnsTrue_whenQualifyingSessionStartsIn20Minutes() {
        // A QUALIFYING session scheduled 20 minutes from now has scheduledAt = now+20min.
        // With corrected window [now-3h, now+30min], now+20min is within range → true.
        // Only stub QUALIFYING; short-circuit OR means SPRINT_SHOOTOUT is never queried.
        when(sessionRepository.existsBySessionTypeAndScheduledAtBetweenAndCompletedFalse(
            eq(Session.SessionType.QUALIFYING), any(), any()))
            .thenReturn(true);

        assertThat(detector.isQualifyingActive()).isTrue();
    }

    @Test
    void isQualifyingActive_returnsTrue_whenSprintShootoutSessionStartsIn20Minutes() {
        // A SPRINT_SHOOTOUT session scheduled 20 minutes from now has scheduledAt = now+20min.
        // With corrected window [now-3h, now+30min], now+20min is within range → true.
        when(sessionRepository.existsBySessionTypeAndScheduledAtBetweenAndCompletedFalse(
            eq(Session.SessionType.QUALIFYING), any(), any()))
            .thenReturn(false);
        when(sessionRepository.existsBySessionTypeAndScheduledAtBetweenAndCompletedFalse(
            eq(Session.SessionType.SPRINT_SHOOTOUT), any(), any()))
            .thenReturn(true);

        assertThat(detector.isQualifyingActive()).isTrue();
    }
}
