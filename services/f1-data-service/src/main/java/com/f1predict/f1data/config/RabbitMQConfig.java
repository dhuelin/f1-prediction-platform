package com.f1predict.f1data.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String F1_EXCHANGE = "f1.events";
    public static final String SESSION_COMPLETE_KEY = "session.complete";
    public static final String RACE_RESULT_KEY = "race.result.final";
    public static final String RESULT_AMENDED_KEY = "race.result.amended";
    public static final String LIVE_POSITION_KEY = "race.live.position";

    @Bean
    public TopicExchange f1Exchange() {
        return new TopicExchange(F1_EXCHANGE);
    }
}
