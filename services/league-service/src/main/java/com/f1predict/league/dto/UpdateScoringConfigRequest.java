package com.f1predict.league.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateScoringConfigRequest(
    @NotNull @Min(1) Integer predictionDepth,
    @NotNull @Min(0) Integer exactPositionPoints,
    String offsetTiers,       // JSON string, null = keep existing
    @NotNull @Min(0) Integer inRangePoints,
    BigDecimal betMultiplier,
    String activeBets,        // JSON string, null = keep existing
    Boolean sprintScoringEnabled,
    Integer maxStakePerBet    // null = unlimited
) {}
