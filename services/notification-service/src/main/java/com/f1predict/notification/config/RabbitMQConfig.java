package com.f1predict.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Upstream exchanges
    public static final String PREDICTION_EXCHANGE   = "prediction.events";
    public static final String F1_EXCHANGE           = "f1.events";

    // Routing keys to subscribe to
    public static final String PREDICTION_LOCKED_KEY = "prediction.locked";
    public static final String SESSION_COMPLETE_KEY  = "session.complete";
    public static final String RACE_RESULT_KEY       = "race.result.final";
    public static final String RESULT_AMENDED_KEY    = "race.result.amended";

    // This service's queues
    public static final String PREDICTION_LOCKED_QUEUE   = "notification-service.prediction-locked";
    public static final String SESSION_COMPLETE_QUEUE    = "notification-service.session-complete";
    public static final String RACE_RESULT_QUEUE         = "notification-service.race-result-final";
    public static final String RESULT_AMENDED_QUEUE      = "notification-service.result-amended";

    // Dead-letter
    public static final String DLX = "notification-service.dlx";

    @Bean public TopicExchange predictionExchange() {
        return new TopicExchange(PREDICTION_EXCHANGE, true, false);
    }
    @Bean public TopicExchange f1Exchange() {
        return new TopicExchange(F1_EXCHANGE, true, false);
    }
    @Bean public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    private Queue durableQueue(String name) {
        return QueueBuilder.durable(name)
            .withArgument("x-dead-letter-exchange", DLX)
            .withArgument("x-dead-letter-routing-key", name)
            .build();
    }

    @Bean public Queue predictionLockedQueue()  { return durableQueue(PREDICTION_LOCKED_QUEUE); }
    @Bean public Queue sessionCompleteQueue()   { return durableQueue(SESSION_COMPLETE_QUEUE); }
    @Bean public Queue raceResultQueue()        { return durableQueue(RACE_RESULT_QUEUE); }
    @Bean public Queue resultAmendedQueue()     { return durableQueue(RESULT_AMENDED_QUEUE); }

    @Bean public Queue predictionLockedDlq() { return new Queue(PREDICTION_LOCKED_QUEUE + ".dlq", true); }
    @Bean public Queue sessionCompleteDlq()  { return new Queue(SESSION_COMPLETE_QUEUE  + ".dlq", true); }
    @Bean public Queue raceResultDlq()       { return new Queue(RACE_RESULT_QUEUE       + ".dlq", true); }
    @Bean public Queue resultAmendedDlq()    { return new Queue(RESULT_AMENDED_QUEUE    + ".dlq", true); }

    @Bean public Binding predictionLockedBinding(Queue predictionLockedQueue, TopicExchange predictionExchange) {
        return BindingBuilder.bind(predictionLockedQueue).to(predictionExchange).with(PREDICTION_LOCKED_KEY);
    }
    @Bean public Binding sessionCompleteBinding(Queue sessionCompleteQueue, TopicExchange f1Exchange) {
        return BindingBuilder.bind(sessionCompleteQueue).to(f1Exchange).with(SESSION_COMPLETE_KEY);
    }
    @Bean public Binding raceResultBinding(Queue raceResultQueue, TopicExchange f1Exchange) {
        return BindingBuilder.bind(raceResultQueue).to(f1Exchange).with(RACE_RESULT_KEY);
    }
    @Bean public Binding resultAmendedBinding(Queue resultAmendedQueue, TopicExchange f1Exchange) {
        return BindingBuilder.bind(resultAmendedQueue).to(f1Exchange).with(RESULT_AMENDED_KEY);
    }

    // DLQ bindings — without these the DirectExchange drops dead-lettered messages
    @Bean public Binding predictionLockedDlqBinding(Queue predictionLockedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(predictionLockedDlq).to(deadLetterExchange).with(PREDICTION_LOCKED_QUEUE);
    }
    @Bean public Binding sessionCompleteDlqBinding(Queue sessionCompleteDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(sessionCompleteDlq).to(deadLetterExchange).with(SESSION_COMPLETE_QUEUE);
    }
    @Bean public Binding raceResultDlqBinding(Queue raceResultDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(raceResultDlq).to(deadLetterExchange).with(RACE_RESULT_QUEUE);
    }
    @Bean public Binding resultAmendedDlqBinding(Queue resultAmendedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(resultAmendedDlq).to(deadLetterExchange).with(RESULT_AMENDED_QUEUE);
    }

    @Bean public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(messageConverter());
        return t;
    }
}
