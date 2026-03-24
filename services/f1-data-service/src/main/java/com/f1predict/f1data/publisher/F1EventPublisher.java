package com.f1predict.f1data.publisher;

import com.f1predict.common.events.*;
import com.f1predict.f1data.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class F1EventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public F1EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishSessionComplete(SessionCompleteEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.F1_EXCHANGE, RabbitMQConfig.SESSION_COMPLETE_KEY, event);
    }

    public void publishRaceResultFinal(RaceResultFinalEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.F1_EXCHANGE, RabbitMQConfig.RACE_RESULT_KEY, event);
    }

    public void publishResultAmended(ResultAmendedEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.F1_EXCHANGE, RabbitMQConfig.RESULT_AMENDED_KEY, event);
    }
}
