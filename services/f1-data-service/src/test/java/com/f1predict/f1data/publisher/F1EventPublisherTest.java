package com.f1predict.f1data.publisher;

import com.f1predict.common.events.RaceResultFinalEvent;
import com.f1predict.common.events.ResultAmendedEvent;
import com.f1predict.common.events.SessionCompleteEvent;
import com.f1predict.f1data.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class F1EventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private F1EventPublisher publisher;

    @Test
    void publishSessionComplete_sendsToCorrectExchangeAndRoutingKey() {
        SessionCompleteEvent event = new SessionCompleteEvent(
            "race-2025-01", SessionCompleteEvent.SessionType.QUALIFYING, "2025", 1);

        publisher.publishSessionComplete(event);

        verify(rabbitTemplate).convertAndSend(
            RabbitMQConfig.F1_EXCHANGE, RabbitMQConfig.SESSION_COMPLETE_KEY, event);
    }

    @Test
    void publishRaceResultFinal_sendsToCorrectExchangeAndRoutingKey() {
        RaceResultFinalEvent event = new RaceResultFinalEvent(
            "race-2025-01",
            SessionCompleteEvent.SessionType.RACE,
            List.of(new RaceResultFinalEvent.DriverResult("VER", 1, RaceResultFinalEvent.DriverStatus.CLASSIFIED)),
            false);

        publisher.publishRaceResultFinal(event);

        verify(rabbitTemplate).convertAndSend(
            RabbitMQConfig.F1_EXCHANGE, RabbitMQConfig.RACE_RESULT_KEY, event);
    }

    @Test
    void publishResultAmended_sendsToCorrectExchangeAndRoutingKey() {
        ResultAmendedEvent event = new ResultAmendedEvent(
            "race-2025-01",
            SessionCompleteEvent.SessionType.RACE,
            List.of(new RaceResultFinalEvent.DriverResult("VER", 2, RaceResultFinalEvent.DriverStatus.CLASSIFIED)),
            "POST_RACE_TIME_PENALTY");

        publisher.publishResultAmended(event);

        verify(rabbitTemplate).convertAndSend(
            RabbitMQConfig.F1_EXCHANGE, RabbitMQConfig.RESULT_AMENDED_KEY, event);
    }
}
