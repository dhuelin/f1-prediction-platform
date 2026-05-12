package com.f1predict.f1data.service;

import com.f1predict.f1data.client.OpenF1Client;
import com.f1predict.f1data.config.RedisConfig;
import com.f1predict.f1data.dto.LivePositionEventDto;
import com.f1predict.f1data.dto.LivePositionEventDto.DriverPositionDto;
import com.f1predict.f1data.dto.openf1.OpenF1PositionDto;
import com.f1predict.f1data.dto.openf1.OpenF1SessionDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LiveSessionService {

    private static final Logger log = LoggerFactory.getLogger(LiveSessionService.class);
    private static final Duration SESSION_KEY_CACHE_TTL = Duration.ofMinutes(5);

    private final OpenF1Client openF1Client;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private int cachedSessionKey = 0;
    private Instant cacheExpiresAt = Instant.EPOCH;

    public LiveSessionService(OpenF1Client openF1Client,
                              StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper) {
        this.openF1Client = openF1Client;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void pollQualifyingState() {
        publishToRedis("Qualifying", "Qualifying", "Sprint Shootout");
    }

    public void pollLivePositions() {
        publishToRedis("Race", "Race", "Sprint");
    }

    // Fetches latest positions from OpenF1 and publishes the event to Redis.
    // All service instances subscribed to the channel will relay it to their WS clients.
    private void publishToRedis(String logLabel, String... sessionTypes) {
        int sessionKey = resolveSessionKey(sessionTypes);
        if (sessionKey == 0) {
            log.debug("{} poll: no active OpenF1 session found", logLabel);
            return;
        }

        List<OpenF1PositionDto> raw;
        try {
            raw = openF1Client.fetchLivePositions(sessionKey);
        } catch (Exception e) {
            log.warn("{} poll: failed to fetch positions for session {}: {}", logLabel, sessionKey, e.getMessage());
            return;
        }

        if (raw.isEmpty()) return;

        // OpenF1 returns all historical position records; keep the latest per driver.
        Map<Integer, Integer> latestByDriver = new LinkedHashMap<>();
        for (OpenF1PositionDto p : raw) {
            if (p.driverNumber() != null && p.position() != null) {
                latestByDriver.put(p.driverNumber(), p.position());
            }
        }

        List<DriverPositionDto> positions = latestByDriver.entrySet().stream()
                .map(e -> new DriverPositionDto(e.getKey(), e.getValue()))
                .sorted((a, b) -> Integer.compare(a.position(), b.position()))
                .toList();

        LivePositionEventDto event = new LivePositionEventDto(sessionKey, Instant.now(), positions);
        String channel = RedisConfig.LIVE_POSITIONS_CHANNEL_PREFIX + sessionKey;
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(event));
            log.debug("{} poll: published {} driver positions to Redis channel {}", logLabel, positions.size(), channel);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize live position event: {}", e.getMessage());
        }
    }

    /**
     * Returns the latest deduplicated positions for the currently active Race or Sprint session,
     * falling back to Qualifying if no race session is active.
     * Returns an empty list when no session is live.
     */
    public List<OpenF1PositionDto> fetchCurrentPositions() {
        int sessionKey = resolveSessionKey("Race", "Sprint");
        if (sessionKey == 0) {
            sessionKey = resolveSessionKey("Qualifying", "Sprint Shootout");
        }
        if (sessionKey == 0) return List.of();

        List<OpenF1PositionDto> raw;
        try {
            raw = openF1Client.fetchLivePositions(sessionKey);
        } catch (Exception e) {
            log.warn("fetchCurrentPositions: failed to fetch positions for session {}: {}", sessionKey, e.getMessage());
            return List.of();
        }

        Map<Integer, OpenF1PositionDto> latest = new LinkedHashMap<>();
        for (OpenF1PositionDto p : raw) {
            if (p.driverNumber() != null && p.position() != null) {
                latest.put(p.driverNumber(), p);
            }
        }
        return List.copyOf(latest.values());
    }

    // Resolves the OpenF1 session key for an active session of the given types.
    // Caches the result for SESSION_KEY_CACHE_TTL to avoid hammering the sessions API.
    private synchronized int resolveSessionKey(String... sessionTypes) {
        if (cachedSessionKey != 0 && Instant.now().isBefore(cacheExpiresAt)) {
            return cachedSessionKey;
        }

        int year = Year.now().getValue();
        Instant now = Instant.now();
        Instant windowStart = now.minus(3, ChronoUnit.HOURS);
        Instant windowEnd = now.plus(30, ChronoUnit.MINUTES);

        List<OpenF1SessionDto> sessions;
        try {
            sessions = openF1Client.fetchSessions(year);
        } catch (Exception e) {
            log.warn("Failed to fetch OpenF1 sessions: {}", e.getMessage());
            return 0;
        }

        int found = sessions.stream()
                .filter(s -> s.sessionKey() != null && s.sessionType() != null && s.dateStart() != null)
                .filter(s -> matchesType(s.sessionType(), sessionTypes))
                .filter(s -> {
                    try {
                        Instant start = Instant.parse(s.dateStart());
                        return start.isAfter(windowStart) && start.isBefore(windowEnd);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .mapToInt(OpenF1SessionDto::sessionKey)
                .findFirst()
                .orElse(0);

        cachedSessionKey = found;
        cacheExpiresAt = Instant.now().plus(SESSION_KEY_CACHE_TTL);
        return found;
    }

    private boolean matchesType(String sessionType, String[] types) {
        for (String t : types) {
            if (t.equalsIgnoreCase(sessionType)) return true;
        }
        return false;
    }
}
