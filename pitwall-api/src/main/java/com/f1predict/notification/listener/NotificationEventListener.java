package com.f1predict.notification.listener;

import com.f1predict.common.events.*;
import com.f1predict.config.RabbitMQConfig;
import com.f1predict.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // In-process event since prediction and notification share a process
    @EventListener
    public void onPredictionLocked(PredictionLockedEvent event) {
        log.info("PredictionLocked: race={} session={}", event.raceId(), event.sessionType());
        notificationService.sendPredictionReminder(event.raceId());
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_SESSION_COMPLETE_QUEUE)
    public void onSessionComplete(SessionCompleteEvent event) {
        if (event.sessionType() == SessionCompleteEvent.SessionType.QUALIFYING) {
            log.info("SessionComplete(QUALIFYING): race={} — sending race start alert", event.raceId());
            notificationService.sendRaceStartAlert(event.raceId());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_RACE_RESULT_QUEUE)
    public void onRaceResultFinal(RaceResultFinalEvent event) {
        log.info("RaceResultFinal: race={} session={}", event.raceId(), event.sessionType());
        notificationService.sendResultsPublished(event.raceId());
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_RESULT_AMENDED_QUEUE)
    public void onResultAmended(ResultAmendedEvent event) {
        log.info("ResultAmended: race={} reason={}", event.raceId(), event.amendmentReason());
        notificationService.sendScoreAmended(event.raceId(), event.amendmentReason());
    }
}
