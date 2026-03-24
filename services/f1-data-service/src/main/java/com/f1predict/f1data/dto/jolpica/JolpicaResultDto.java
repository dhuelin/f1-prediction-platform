package com.f1predict.f1data.dto.jolpica;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JolpicaResultDto(
    @JsonProperty("position") String position,
    @JsonProperty("Driver") Driver driver,
    @JsonProperty("status") String status,
    @JsonProperty("FastestLap") FastestLap fastestLap
) {
    public record Driver(@JsonProperty("code") String code) {}
    public record FastestLap(@JsonProperty("rank") String rank) {}

    public boolean isFastestLap() {
        return fastestLap != null && "1".equals(fastestLap.rank());
    }
}
