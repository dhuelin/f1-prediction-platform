package com.f1predict.f1data.dto.openf1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenF1PositionDto(
    @JsonProperty("driver_number") Integer driverNumber,
    @JsonProperty("position") Integer position,
    @JsonProperty("date") String date
) {}
