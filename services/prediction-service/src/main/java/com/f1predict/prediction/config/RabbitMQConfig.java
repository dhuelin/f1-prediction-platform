package com.f1predict.prediction.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange declared by F1 Data Service — we just bind to it
    public static final String F1_EXCHANGE = "f1.events";
    public static final String SESSION_COMPLETE_KEY = "session.complete";

    // Exchange declared by this service for outbound events
    public static final String PREDICTION_EXCHANGE = "prediction.events";
    public static final String PREDICTION_LOCKED_KEY = "prediction.locked";

    public static final String SESSION_COMPLETE_QUEUE = "prediction-service.session-complete";

    @Bean
    public TopicExchange f1Exchange() {
        return new TopicExchange(F1_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange predictionExchange() {
        return new TopicExchange(PREDICTION_EXCHANGE, true, false);
    }

    @Bean
    public Queue sessionCompleteQueue() {
        return new Queue(SESSION_COMPLETE_QUEUE, true);
    }

    @Bean
    public Binding sessionCompleteBinding(Queue sessionCompleteQueue, TopicExchange f1Exchange) {
        return BindingBuilder.bind(sessionCompleteQueue).to(f1Exchange).with(SESSION_COMPLETE_KEY);
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
