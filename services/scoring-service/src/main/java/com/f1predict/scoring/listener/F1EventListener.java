package com.f1predict.scoring.listener;

import com.f1predict.common.events.RaceResultFinalEvent;
import com.f1predict.common.events.ResultAmendedEvent;
import com.f1predict.common.events.SessionCompleteEvent;
import com.f1predict.scoring.config.RabbitMQConfig;
import com.f1predict.scoring.service.ScoringOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class F1EventListener {

    private static final Logger log = LoggerFactory.getLogger(F1EventListener.class);

    private final ScoringOrchestrator orchestrator;

    public F1EventListener(ScoringOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /** #76 — Handle RACE_RESULT_FINAL */
    @RabbitListener(queues = RabbitMQConfig.RACE_RESULT_QUEUE)
    public void onRaceResultFinal(RaceResultFinalEvent event) {
        log.info("Received RACE_RESULT_FINAL: race={} session={} partialDistance={}",
            event.raceId(), event.sessionType(), event.isPartialDistance());
        try {
            // raceNumber is not in the event — pass 1 as fallback; config will use latest effective
            orchestrator.scoreRace(
                event.raceId(),
                event.sessionType(),
                event.results(),
                event.isPartialDistance(),
                false,
                1);
        } catch (Exception e) {
            log.error("Failed to score race {} — message will be dead-lettered", event.raceId(), e);
            throw e; // re-throw so AMQP dead-letters it
        }
    }

    /** #77 — Handle RESULT_AMENDED */
    @RabbitListener(queues = RabbitMQConfig.RESULT_AMENDED_QUEUE)
    public void onResultAmended(ResultAmendedEvent event) {
        log.info("Received RESULT_AMENDED: race={} session={} reason={}",
            event.raceId(), event.sessionType(), event.amendmentReason());
        try {
            orchestrator.scoreRace(
                event.raceId(),
                event.sessionType(),
                event.amendedResults(),
                false,
                false,
                1);
        } catch (Exception e) {
            log.error("Failed to re-score race {} after amendment — message will be dead-lettered", event.raceId(), e);
            throw e;
        }
    }
}
