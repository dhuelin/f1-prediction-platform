package com.f1predict.common.events;

public record SessionCompleteEvent(
    String raceId,
    SessionType sessionType,
    String season,
    int round
) {
    public enum SessionType {
        QUALIFYING, RACE, SPRINT, SPRINT_SHOOTOUT
    }
}
