package com.f1predict.f1data.dto.jolpica;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JolpicaResponseDto(@JsonProperty("MRData") MrData mrData) {
    public record MrData(@JsonProperty("RaceTable") RaceTable raceTable) {
        public record RaceTable(
            @JsonProperty("season") String season,
            @JsonProperty("Races") List<JolpicaRaceDto> races
        ) {}
    }
}
