package com.f1predict.common.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RaceResultFinalEvent(
    String raceId,
    SessionCompleteEvent.SessionType sessionType,
    List<DriverResult> results,
    @JsonProperty("isPartialDistance") boolean isPartialDistance,
    String fastestLapDriver,
    int safetyCarsDeployed
) {
    public record DriverResult(
        String driverCode,
        int finishPosition,
        DriverStatus status
    ) {}

    public enum DriverStatus { CLASSIFIED, DNF, DSQ, DNS }
}
