package com.f1predict.scoring.dto;

import java.math.BigDecimal;
import java.util.List;

public record ScoringConfigData(
    int predictionDepth,
    int exactPositionPoints,
    List<OffsetTier> offsetTiers,
    int inRangePoints,
    BigDecimal betMultiplier,
    boolean fastestLapEnabled,
    boolean dnfDsqDnsEnabled,
    boolean scDeployedEnabled,
    boolean scCountEnabled,
    boolean sprintScoringEnabled,
    Integer maxStakePerBet
) {}
