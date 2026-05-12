package com.f1predict.scoring.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.f1predict.scoring.dto.LivePositionData;
import org.springframework.core.ParameterizedTypeReference;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component
public class F1DataClient {

    private static final Logger log = LoggerFactory.getLogger(F1DataClient.class);

    private final RestClient restClient;

    public F1DataClient(@Value("${services.f1data-url}") String f1dataUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restClient = RestClient.builder()
            .baseUrl(f1dataUrl)
            .requestFactory(factory)
            .build();
    }

    record DeadlineResponse(String raceId, Instant qualifyingDeadline) {}
    record LivePositionsResponse(List<LivePositionData> positions) {}

    /**
     * Returns the current live driver positions, sorted by position ascending.
     * Returns empty list when no session is active or the service is unavailable.
     */
    public List<LivePositionData> getLivePositions() {
        try {
            LivePositionsResponse response = restClient.get()
                .uri("/live/positions")
                .retrieve()
                .body(LivePositionsResponse.class);
            return response != null ? response.positions() : Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("F1 data service unavailable for live positions — projected score skipped");
            return Collections.emptyList();
        }
    }

    /**
     * Returns the qualifying deadline for the race, or null if the race is unknown
     * or the F1 data service is unavailable (fail open — caller should score everything).
     */
    public Instant getQualifyingDeadline(String raceId) {
        try {
            DeadlineResponse response = restClient.get()
                .uri("/races/{raceId}/deadline", raceId)
                .retrieve()
                .body(DeadlineResponse.class);
            return response != null ? response.qualifyingDeadline() : null;
        } catch (RestClientException e) {
            log.warn("F1 data service unavailable for deadline check — scoring all predictions. raceId={}", raceId);
            return null;
        }
    }
}
