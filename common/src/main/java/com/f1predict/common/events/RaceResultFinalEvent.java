package com.f1predict.common.events;

import java.util.List;

public record RaceResultFinalEvent(
    String raceId,
    SessionCompleteEvent.SessionType sessionType,
    List<DriverResult> results,
    boolean isPartialDistance
) {
    public record DriverResult(
        String driverCode,
        int finishPosition,
        DriverStatus status
    ) {}

    public enum DriverStatus { CLASSIFIED, DNF, DSQ, DNS }
}
