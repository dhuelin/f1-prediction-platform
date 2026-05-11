package com.f1predict.f1data.dto;

import java.time.Instant;
import java.util.List;

public record LivePositionEventDto(
    int sessionKey,
    Instant timestamp,
    List<DriverPositionDto> positions
) {
    public record DriverPositionDto(int driverNumber, int position) {}
}
