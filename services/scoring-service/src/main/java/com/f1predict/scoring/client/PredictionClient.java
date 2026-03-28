package com.f1predict.scoring.client;

import com.f1predict.scoring.dto.PredictionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

@Component
public class PredictionClient {

    private static final Logger log = LoggerFactory.getLogger(PredictionClient.class);

    private final RestClient restClient;

    public PredictionClient(@Value("${services.prediction-url}") String predictionUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder()
            .baseUrl(predictionUrl)
            .requestFactory(factory)
            .build();
    }

    /**
     * Fetch all locked predictions for a race and session type.
     * Returns empty list on failure.
     */
    public List<PredictionData> getPredictions(String raceId, String sessionType) {
        try {
            List<PredictionData> result = restClient.get()
                .uri("/predictions/{raceId}/internal/all?sessionType={sessionType}", raceId, sessionType)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
            return result != null ? result : Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Failed to fetch predictions for race {} session {} — skipping scoring", raceId, sessionType, e);
            return Collections.emptyList();
        }
    }
}
