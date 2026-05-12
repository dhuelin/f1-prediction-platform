package com.f1predict.f1data.config;

import com.f1predict.f1data.pubsub.LivePositionRedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

    // All live position channels: f1.live.positions.<sessionKey>
    public static final String LIVE_POSITIONS_CHANNEL_PREFIX = "f1.live.positions.";
    public static final String LIVE_POSITIONS_PATTERN = LIVE_POSITIONS_CHANNEL_PREFIX + "*";

    @Bean
    public RedisMessageListenerContainer redisListenerContainer(
            RedisConnectionFactory connectionFactory,
            LivePositionRedisSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new PatternTopic(LIVE_POSITIONS_PATTERN));
        return container;
    }
}
