package com.f1predict.f1data.dto.openf1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1LapDto(
    @JsonProperty("driver_number") Integer driverNumber,
    @JsonProperty("lap_duration") Double lapDuration,
    @JsonProperty("is_fastest_lap") Boolean isFastestLap
) {}
