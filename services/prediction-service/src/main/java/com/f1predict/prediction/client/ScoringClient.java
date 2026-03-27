package com.f1predict.prediction.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class ScoringClient {

    private static final Logger log = LoggerFactory.getLogger(ScoringClient.class);

    private final RestClient restClient;

    public ScoringClient(@Value("${services.scoring-url}") String scoringUrl) {
        this.restClient = RestClient.builder().baseUrl(scoringUrl).build();
    }

    /**
     * Returns the user's current points balance in a league.
     * Returns Integer.MAX_VALUE if scoring service is unavailable (fail open).
     */
    public int getBalance(UUID userId, UUID leagueId) {
        try {
            Integer balance = restClient.get()
                .uri("/scores/balance/{userId}/leagues/{leagueId}", userId, leagueId)
                .retrieve()
                .body(Integer.class);
            return balance != null ? balance : Integer.MAX_VALUE;
        } catch (RestClientException e) {
            log.warn("Scoring service unavailable for balance check — allowing submission. userId={}", userId);
            return Integer.MAX_VALUE;
        }
    }
}
