package com.f1predict.scoring.publisher;

import com.f1predict.common.events.SessionCompleteEvent;
import com.f1predict.common.events.StandingsUpdatedEvent;
import com.f1predict.scoring.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScoringEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ScoringEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishStandingsUpdated(String raceId,
                                        SessionCompleteEvent.SessionType sessionType,
                                        UUID leagueId,
                                        int memberCount) {
        StandingsUpdatedEvent event = new StandingsUpdatedEvent(
            raceId, sessionType, leagueId.toString(), memberCount);
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SCORING_EXCHANGE,
                RabbitMQConfig.STANDINGS_UPDATED_KEY,
                event);
        } catch (Exception e) {
            // Log but don't fail — standings are persisted; downstream consumers will catch up
            org.slf4j.LoggerFactory.getLogger(ScoringEventPublisher.class)
                .warn("Failed to publish StandingsUpdatedEvent for race {} league {}", raceId, leagueId, e);
        }
    }
}
