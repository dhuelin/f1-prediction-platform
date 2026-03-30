package com.f1predict.prediction.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InternalPredictionResponse(
    UUID userId,
    String sessionType,
    List<String> rankedDriverCodes,
    List<InternalBetData> bets,
    Instant updatedAt
) {
    public record InternalBetData(String betType, int stake, String betValue) {}
}
