package com.f1predict.f1data.scheduler;

import com.f1predict.f1data.service.RaceCalendarService;
import com.f1predict.f1data.service.LiveSessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Year;

@Component
public class F1DataPoller {
    private final RaceCalendarService calendarService;
    private final LiveSessionService liveSessionService;
    private final RaceWeekendDetector weekendDetector;

    public F1DataPoller(RaceCalendarService calendarService,
                        LiveSessionService liveSessionService,
                        RaceWeekendDetector weekendDetector) {
        this.calendarService = calendarService;
        this.liveSessionService = liveSessionService;
        this.weekendDetector = weekendDetector;
    }

    // Off-weekend: daily sync at 6am
    @Scheduled(cron = "0 0 6 * * *")
    public void dailyCalendarSync() {
        if (!weekendDetector.isRaceWeekend()) {
            calendarService.syncCalendar(Year.now().getValue());
        }
    }

    // Race weekend: every 5 minutes
    @Scheduled(fixedDelay = 300_000)
    public void raceWeekendPoll() {
        if (weekendDetector.isRaceWeekend() && !weekendDetector.isSessionActive()) {
            calendarService.syncCalendar(Year.now().getValue());
        }
    }

    // Active qualifying: every 30 seconds
    @Scheduled(fixedDelay = 30_000)
    public void qualifyingPoll() {
        if (weekendDetector.isQualifyingActive()) {
            liveSessionService.pollQualifyingState();
        }
    }

    // Active race: every 5 seconds
    @Scheduled(fixedDelay = 5_000)
    public void liveRacePoll() {
        if (weekendDetector.isRaceActive()) {
            liveSessionService.pollLivePositions();
        }
    }
}
