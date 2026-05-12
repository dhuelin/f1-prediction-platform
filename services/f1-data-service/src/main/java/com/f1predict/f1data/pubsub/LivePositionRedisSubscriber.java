package com.f1predict.f1data.pubsub;

import com.f1predict.f1data.dto.LivePositionEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class LivePositionRedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(LivePositionRedisSubscriber.class);
    private static final String LIVE_TOPIC = "/topic/live/";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public LivePositionRedisSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            LivePositionEventDto event = objectMapper.readValue(message.getBody(), LivePositionEventDto.class);
            messagingTemplate.convertAndSend(LIVE_TOPIC + event.sessionKey(), event);
            log.debug("Relayed live positions for session {} to WS topic", event.sessionKey());
        } catch (Exception e) {
            log.warn("Failed to process live position message from Redis: {}", e.getMessage());
        }
    }
}
