package com.f1predict.league.api;

import java.math.BigDecimal;

/**
 * Scoring rules as the league domain stores them. offsetTiersJson and
 * activeBetsJson are kept as raw JSON exactly as they are persisted; the scoring
 * domain parses them, as it did when this arrived over HTTP.
 */
public record LeagueScoringConfig(
    int predictionDepth,
    int exactPositionPoints,
    String offsetTiersJson,
    int inRangePoints,
    BigDecimal betMultiplier,
    String activeBetsJson,
    boolean sprintScoringEnabled,
    Integer maxStakePerBet
) {}
