package com.f1predict.prediction.publisher;

import com.f1predict.common.events.PredictionLockedEvent;
import com.f1predict.common.events.SessionCompleteEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes PredictionLockedEvent to the notification and analytics domains.
 *
 * Was a RabbitMQ publish to the prediction.events exchange; both consumers now
 * live in this process, so it is a Spring ApplicationEvent. Delivery is
 * synchronous and in the caller's transaction, so a consumer that throws will
 * roll the caller back — previously the broker absorbed that.
 */
@Component
public class PredictionEventPublisher {

    private final ApplicationEventPublisher events;

    public PredictionEventPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    public void publishPredictionLocked(String raceId, SessionCompleteEvent.SessionType sessionType, int lockedCount) {
        events.publishEvent(new PredictionLockedEvent(raceId, sessionType, lockedCount));
    }
}
