package com.f1predict.f1data.client;

import com.f1predict.f1data.dto.jolpica.JolpicaRaceDto;
import com.f1predict.f1data.dto.jolpica.JolpicaResponseDto;
import com.f1predict.f1data.dto.jolpica.JolpicaResultDto;
import com.f1predict.f1data.dto.jolpica.JolpicaResultResponseDto;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

public class JolpicaClient {
    private final RestClient restClient;

    public JolpicaClient(String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<JolpicaRaceDto> fetchCalendar(int season) {
        var response = restClient.get()
            .uri("/ergast/f1/{season}.json", season)
            .retrieve()
            .body(JolpicaResponseDto.class);
        if (response == null || response.mrData().raceTable().races() == null) {
            return Collections.emptyList();
        }
        return response.mrData().raceTable().races();
    }

    public List<JolpicaResultDto> fetchRaceResults(int season, int round) {
        var response = restClient.get()
            .uri("/ergast/f1/{season}/{round}/results.json", season, round)
            .retrieve()
            .body(JolpicaResultResponseDto.class);
        if (response == null) return Collections.emptyList();
        var races = response.mrData().raceTable().races();
        if (races == null || races.isEmpty()) return Collections.emptyList();
        var results = races.get(0).results();
        return results != null ? results : Collections.emptyList();
    }
}
