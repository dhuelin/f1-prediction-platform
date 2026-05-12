package com.f1predict.analytics.listener;

import com.f1predict.analytics.config.RabbitMQConfig;
import com.f1predict.analytics.model.RaceAnalyticsEvent;
import com.f1predict.analytics.repository.RaceAnalyticsEventRepository;
import com.f1predict.common.events.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsEventListener {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventListener.class);

    private final RaceAnalyticsEventRepository repository;
    private final ObjectMapper objectMapper;

    public AnalyticsEventListener(RaceAnalyticsEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.PREDICTION_LOCKED_QUEUE)
    public void onPredictionLocked(PredictionLockedEvent event) {
        log.info("analytics: PredictionLocked race={} lockedCount={}", event.raceId(), event.lockedCount());
        persist("PREDICTION_LOCKED", event.raceId(), null, toJson(event));
    }

    @RabbitListener(queues = RabbitMQConfig.RACE_RESULT_QUEUE)
    public void onRaceResultFinal(RaceResultFinalEvent event) {
        log.info("analytics: RaceResultFinal race={} session={}", event.raceId(), event.sessionType());
        persist("RACE_RESULT_FINAL", event.raceId(), null, toJson(event));
    }

    @RabbitListener(queues = RabbitMQConfig.STANDINGS_UPDATED_QUEUE)
    public void onStandingsUpdated(StandingsUpdatedEvent event) {
        log.info("analytics: StandingsUpdated race={} league={} members={}", event.raceId(), event.leagueId(), event.memberCount());
        persist("STANDINGS_UPDATED", event.raceId(), event.leagueId(), toJson(event));
    }

    @RabbitListener(queues = RabbitMQConfig.SESSION_COMPLETE_QUEUE)
    public void onSessionComplete(SessionCompleteEvent event) {
        log.info("analytics: SessionComplete race={} session={}", event.raceId(), event.sessionType());
        persist("SESSION_COMPLETE", event.raceId(), null, toJson(event));
    }

    private void persist(String eventType, String raceId, String leagueId, String payload) {
        try {
            repository.save(new RaceAnalyticsEvent(eventType, raceId, leagueId, payload));
        } catch (Exception e) {
            log.error("analytics: failed to persist event type={} race={}", eventType, raceId, e);
            throw e;
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("analytics: could not serialize event {}", obj.getClass().getSimpleName(), e);
            return "{}";
        }
    }
}
