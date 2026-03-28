package com.f1predict.scoring.dto;

import java.util.List;
import java.util.UUID;

public record PredictionData(
    UUID userId,
    String sessionType,
    List<String> rankedDriverCodes,
    List<BetData> bets
) {}
