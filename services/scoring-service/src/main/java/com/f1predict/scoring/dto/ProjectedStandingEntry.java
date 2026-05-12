package com.f1predict.scoring.dto;

import java.util.UUID;

public record ProjectedStandingEntry(
    UUID userId,
    int projectedRacePoints,
    int currentLeaguePoints
) {}
