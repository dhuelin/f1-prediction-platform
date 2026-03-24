package com.f1predict.f1data.dto.jolpica;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JolpicaRaceDto(
    @JsonProperty("round") String round,
    @JsonProperty("raceName") String raceName,
    @JsonProperty("Circuit") Circuit circuit,
    @JsonProperty("date") String date,
    @JsonProperty("time") String time,
    @JsonProperty("Sprint") SprintInfo sprint
) {
    public record SprintInfo(
        @JsonProperty("date") String date,
        @JsonProperty("time") String time
    ) {}

    public record Circuit(
        @JsonProperty("circuitName") String circuitName,
        @JsonProperty("Location") Location location
    ) {
        public record Location(@JsonProperty("country") String country) {}
    }

    public boolean isSprintWeekend() { return sprint != null; }
}
