package com.f1predict.scoring.publisher;

import com.f1predict.common.events.SessionCompleteEvent;
import com.f1predict.common.events.StandingsUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes StandingsUpdatedEvent to the analytics domain.
 *
 * Was a RabbitMQ publish to the scoring.events exchange; analytics now lives in
 * this process. The publish stays wrapped in a try/catch for the same reason it
 * was before: standings are already persisted at this point, and a failure to
 * record the analytics trail must not undo them.
 */
@Component
public class ScoringEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ScoringEventPublisher.class);

    private final ApplicationEventPublisher events;

    public ScoringEventPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    public void publishStandingsUpdated(String raceId,
                                        SessionCompleteEvent.SessionType sessionType,
                                        UUID leagueId,
                                        int memberCount) {
        try {
            events.publishEvent(new StandingsUpdatedEvent(
                raceId, sessionType, leagueId.toString(), memberCount));
        } catch (Exception e) {
            log.warn("Failed to publish StandingsUpdatedEvent for race {} league {}", raceId, leagueId, e);
        }
    }
}
