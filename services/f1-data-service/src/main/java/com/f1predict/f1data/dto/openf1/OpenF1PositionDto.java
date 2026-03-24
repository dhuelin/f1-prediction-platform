package com.f1predict.f1data.dto.openf1;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OpenF1PositionDto(
    @JsonProperty("driver_number") int driverNumber,
    @JsonProperty("position") int position,
    @JsonProperty("date") String date
) {}
