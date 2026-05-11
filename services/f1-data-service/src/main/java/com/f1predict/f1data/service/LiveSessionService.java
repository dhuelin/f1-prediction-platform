package com.f1predict.f1data.service;

import com.f1predict.f1data.client.OpenF1Client;
import com.f1predict.f1data.dto.LivePositionEventDto;
import com.f1predict.f1data.dto.LivePositionEventDto.DriverPositionDto;
import com.f1predict.f1data.dto.openf1.OpenF1PositionDto;
import com.f1predict.f1data.dto.openf1.OpenF1SessionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private static final String LIVE_TOPIC = "/topic/live/";

    private final OpenF1Client openF1Client;
    private final SimpMessagingTemplate messagingTemplate;

    private int cachedSessionKey = 0;
    private Instant cacheExpiresAt = Instant.EPOCH;

    public LiveSessionService(OpenF1Client openF1Client, SimpMessagingTemplate messagingTemplate) {
        this.openF1Client = openF1Client;
        this.messagingTemplate = messagingTemplate;
    }

    public void pollQualifyingState() {
        broadcast("Qualifying", "Qualifying", "Sprint Shootout");
    }

    public void pollLivePositions() {
        broadcast("Race", "Race", "Sprint");
    }

    private void broadcast(String logLabel, String... sessionTypes) {
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
        messagingTemplate.convertAndSend(LIVE_TOPIC + sessionKey, event);
        log.debug("{} poll: broadcasted {} driver positions for session {}", logLabel, positions.size(), sessionKey);
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
