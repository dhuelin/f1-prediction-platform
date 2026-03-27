package com.f1predict.common.events;

// Published by Scoring Service after league standings are recalculated
public record StandingsUpdatedEvent(
    String raceId,
    SessionCompleteEvent.SessionType sessionType,
    String leagueId,
    int memberCount  // number of members whose standings were updated
) {}
