package com.f1predict.common.events;

import java.util.List;

// Valid amendmentReason values:
//   "POST_RACE_TIME_PENALTY" - driver received a time penalty after the race
//   "POST_RACE_DSQ"          - driver was disqualified after the race
//   "FASTEST_LAP_AMENDED"    - the fastest lap attribution was corrected
public record ResultAmendedEvent(
    String raceId,
    SessionCompleteEvent.SessionType sessionType,
    List<RaceResultFinalEvent.DriverResult> amendedResults,
    String amendmentReason,
    String fastestLapDriver,
    int safetyCarsDeployed
) {}
