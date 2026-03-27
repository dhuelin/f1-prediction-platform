package com.f1predict.league.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class ScoringClient {

    private static final Logger log = LoggerFactory.getLogger(ScoringClient.class);

    private final RestClient restClient;

    public ScoringClient(@Value("${services.scoring-url}") String scoringUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.restClient = RestClient.builder()
            .baseUrl(scoringUrl)
            .requestFactory(factory)
            .build();
    }

    /**
     * Returns the average total_points across all members of a league.
     * Returns 0 if scoring service is unavailable (new member gets no catch-up points).
     */
    public int getLeagueAveragePoints(UUID leagueId) {
        try {
            Integer avg = restClient.get()
                .uri("/scores/leagues/{leagueId}/average-points", leagueId)
                .retrieve()
                .body(Integer.class);
            return avg != null ? avg : 0;
        } catch (RestClientException e) {
            log.warn("Scoring service unavailable for catch-up points — defaulting to 0. leagueId={}", leagueId);
            return 0;
        }
    }
}
