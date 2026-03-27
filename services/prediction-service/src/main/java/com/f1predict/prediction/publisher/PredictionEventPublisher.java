package com.f1predict.prediction.publisher;

import com.f1predict.common.events.PredictionLockedEvent;
import com.f1predict.common.events.SessionCompleteEvent;
import com.f1predict.prediction.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PredictionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PredictionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPredictionLocked(String raceId, SessionCompleteEvent.SessionType sessionType, int lockedCount) {
        PredictionLockedEvent event = new PredictionLockedEvent(raceId, sessionType, lockedCount);
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.PREDICTION_EXCHANGE,
            RabbitMQConfig.PREDICTION_LOCKED_KEY,
            event);
    }
}
