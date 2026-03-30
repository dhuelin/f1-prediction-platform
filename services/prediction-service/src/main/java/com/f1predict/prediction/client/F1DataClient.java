package com.f1predict.prediction.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

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

    /**
     * Returns the qualifying deadline for the race, or null if the race is unknown
     * or the F1 data service is unavailable (fail open — caller should allow the operation).
     */
    public Instant getQualifyingDeadline(String raceId) {
        try {
            DeadlineResponse response = restClient.get()
                .uri("/races/{raceId}/deadline", raceId)
                .retrieve()
                .body(DeadlineResponse.class);
            return response != null ? response.qualifyingDeadline() : null;
        } catch (RestClientException e) {
            log.warn("F1 data service unavailable for deadline check — allowing submission. raceId={}", raceId);
            return null;
        }
    }
}
