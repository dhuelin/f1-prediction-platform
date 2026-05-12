package com.f1predict.analytics.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Upstream exchanges
    public static final String PREDICTION_EXCHANGE = "prediction.events";
    public static final String F1_EXCHANGE         = "f1.events";
    public static final String SCORING_EXCHANGE    = "scoring.events";

    // Routing keys
    public static final String PREDICTION_LOCKED_KEY = "prediction.locked";
    public static final String RACE_RESULT_KEY       = "race.result.final";
    public static final String STANDINGS_UPDATED_KEY = "standings.updated";
    public static final String SESSION_COMPLETE_KEY  = "session.complete";

    // This service's queues
    public static final String PREDICTION_LOCKED_QUEUE  = "analytics-service.prediction-locked";
    public static final String RACE_RESULT_QUEUE        = "analytics-service.race-result-final";
    public static final String STANDINGS_UPDATED_QUEUE  = "analytics-service.standings-updated";
    public static final String SESSION_COMPLETE_QUEUE   = "analytics-service.session-complete";

    // Dead-letter exchange
    public static final String DLX = "analytics-service.dlx";

    @Bean public TopicExchange predictionExchange() {
        return new TopicExchange(PREDICTION_EXCHANGE, true, false);
    }
    @Bean public TopicExchange f1Exchange() {
        return new TopicExchange(F1_EXCHANGE, true, false);
    }
    @Bean public TopicExchange scoringExchange() {
        return new TopicExchange(SCORING_EXCHANGE, true, false);
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

    @Bean public Queue predictionLockedQueue() { return durableQueue(PREDICTION_LOCKED_QUEUE); }
    @Bean public Queue raceResultQueue()       { return durableQueue(RACE_RESULT_QUEUE); }
    @Bean public Queue standingsUpdatedQueue() { return durableQueue(STANDINGS_UPDATED_QUEUE); }
    @Bean public Queue sessionCompleteQueue()  { return durableQueue(SESSION_COMPLETE_QUEUE); }

    @Bean public Queue predictionLockedDlq() { return new Queue(PREDICTION_LOCKED_QUEUE + ".dlq", true); }
    @Bean public Queue raceResultDlq()       { return new Queue(RACE_RESULT_QUEUE       + ".dlq", true); }
    @Bean public Queue standingsUpdatedDlq() { return new Queue(STANDINGS_UPDATED_QUEUE + ".dlq", true); }
    @Bean public Queue sessionCompleteDlq()  { return new Queue(SESSION_COMPLETE_QUEUE  + ".dlq", true); }

    @Bean public Binding predictionLockedBinding(Queue predictionLockedQueue, TopicExchange predictionExchange) {
        return BindingBuilder.bind(predictionLockedQueue).to(predictionExchange).with(PREDICTION_LOCKED_KEY);
    }
    @Bean public Binding raceResultBinding(Queue raceResultQueue, TopicExchange f1Exchange) {
        return BindingBuilder.bind(raceResultQueue).to(f1Exchange).with(RACE_RESULT_KEY);
    }
    @Bean public Binding standingsUpdatedBinding(Queue standingsUpdatedQueue, TopicExchange scoringExchange) {
        return BindingBuilder.bind(standingsUpdatedQueue).to(scoringExchange).with(STANDINGS_UPDATED_KEY);
    }
    @Bean public Binding sessionCompleteBinding(Queue sessionCompleteQueue, TopicExchange f1Exchange) {
        return BindingBuilder.bind(sessionCompleteQueue).to(f1Exchange).with(SESSION_COMPLETE_KEY);
    }

    @Bean public Binding predictionLockedDlqBinding(Queue predictionLockedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(predictionLockedDlq).to(deadLetterExchange).with(PREDICTION_LOCKED_QUEUE);
    }
    @Bean public Binding raceResultDlqBinding(Queue raceResultDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(raceResultDlq).to(deadLetterExchange).with(RACE_RESULT_QUEUE);
    }
    @Bean public Binding standingsUpdatedDlqBinding(Queue standingsUpdatedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(standingsUpdatedDlq).to(deadLetterExchange).with(STANDINGS_UPDATED_QUEUE);
    }
    @Bean public Binding sessionCompleteDlqBinding(Queue sessionCompleteDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(sessionCompleteDlq).to(deadLetterExchange).with(SESSION_COMPLETE_QUEUE);
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
