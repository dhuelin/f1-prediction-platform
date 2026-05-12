package com.f1predict.f1data.dto;

import java.time.Instant;
import java.util.List;

public record LivePositionEventDto(
    int sessionKey,
    String raceId,       // nullable — null when no matching DB session found
    Instant timestamp,
    List<DriverPositionDto> positions
) {
    public record DriverPositionDto(int driverNumber, String driverCode, int position) {}
}
