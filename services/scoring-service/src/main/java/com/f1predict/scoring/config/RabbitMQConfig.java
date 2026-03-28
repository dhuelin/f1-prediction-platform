package com.f1predict.scoring.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String F1_EXCHANGE          = "f1.events";
    public static final String RACE_RESULT_KEY      = "race.result.final";
    public static final String RESULT_AMENDED_KEY   = "race.result.amended";

    public static final String SCORING_EXCHANGE     = "scoring.events";
    public static final String STANDINGS_UPDATED_KEY = "standings.updated";

    public static final String RACE_RESULT_QUEUE    = "scoring-service.race-result-final";
    public static final String RESULT_AMENDED_QUEUE = "scoring-service.result-amended";

    public static final String DLX                  = "scoring-service.dlx";
    public static final String RACE_RESULT_DLQ      = "scoring-service.race-result-final.dlq";
    public static final String RESULT_AMENDED_DLQ   = "scoring-service.result-amended.dlq";

    @Bean
    public TopicExchange f1Exchange() {
        return new TopicExchange(F1_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange scoringExchange() {
        return new TopicExchange(SCORING_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue raceResultQueue() {
        return QueueBuilder.durable(RACE_RESULT_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX)
            .withArgument("x-dead-letter-routing-key", RACE_RESULT_QUEUE)
            .build();
    }

    @Bean
    public Queue resultAmendedQueue() {
        return QueueBuilder.durable(RESULT_AMENDED_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX)
            .withArgument("x-dead-letter-routing-key", RESULT_AMENDED_QUEUE)
            .build();
    }

    @Bean
    public Queue raceResultDlq() {
        return new Queue(RACE_RESULT_DLQ, true);
    }

    @Bean
    public Queue resultAmendedDlq() {
        return new Queue(RESULT_AMENDED_DLQ, true);
    }

    @Bean
    public Binding raceResultBinding(Queue raceResultQueue, TopicExchange f1Exchange) {
        return BindingBuilder.bind(raceResultQueue).to(f1Exchange).with(RACE_RESULT_KEY);
    }

    @Bean
    public Binding resultAmendedBinding(Queue resultAmendedQueue, TopicExchange f1Exchange) {
        return BindingBuilder.bind(resultAmendedQueue).to(f1Exchange).with(RESULT_AMENDED_KEY);
    }

    @Bean
    public Binding raceResultDlqBinding(Queue raceResultDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(raceResultDlq).to(deadLetterExchange).with(RACE_RESULT_QUEUE);
    }

    @Bean
    public Binding resultAmendedDlqBinding(Queue resultAmendedDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(resultAmendedDlq).to(deadLetterExchange).with(RESULT_AMENDED_QUEUE);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
