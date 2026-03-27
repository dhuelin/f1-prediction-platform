package com.f1predict.league.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ScoringConfigResponse(
    UUID id,
    int effectiveFromRace,
    int predictionDepth,
    int exactPositionPoints,
    String offsetTiers,
    int inRangePoints,
    BigDecimal betMultiplier,
    String activeBets,
    boolean sprintScoringEnabled,
    Integer maxStakePerBet
) {}
