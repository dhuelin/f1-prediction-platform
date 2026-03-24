package com.f1predict.f1data.dto.jolpica;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record JolpicaResultResponseDto(@JsonProperty("MRData") MrData mrData) {
    public record MrData(@JsonProperty("RaceTable") RaceTable raceTable) {
        public record RaceTable(@JsonProperty("Races") List<RaceWithResults> races) {
            public record RaceWithResults(@JsonProperty("Results") List<JolpicaResultDto> results) {}
        }
    }
}
