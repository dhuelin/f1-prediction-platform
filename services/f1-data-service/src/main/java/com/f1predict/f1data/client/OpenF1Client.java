package com.f1predict.f1data.client;

import com.f1predict.f1data.dto.openf1.OpenF1PositionDto;
import com.f1predict.f1data.dto.openf1.OpenF1SessionDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

public class OpenF1Client {
    private final RestClient restClient;

    public OpenF1Client(String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<OpenF1SessionDto> fetchSessions(int year) {
        var result = restClient.get()
            .uri("/v1/sessions?year={year}", year)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, resp) -> { throw new F1ApiException("F1 API error: " + resp.getStatusCode(), null); })
            .body(new ParameterizedTypeReference<List<OpenF1SessionDto>>() {});
        return result != null ? result : Collections.emptyList();
    }

    public List<OpenF1PositionDto> fetchLivePositions(int sessionKey) {
        var result = restClient.get()
            .uri("/v1/position?session_key={key}", sessionKey)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, resp) -> { throw new F1ApiException("F1 API error: " + resp.getStatusCode(), null); })
            .body(new ParameterizedTypeReference<List<OpenF1PositionDto>>() {});
        return result != null ? result : Collections.emptyList();
    }
}
