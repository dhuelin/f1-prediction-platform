package com.f1predict.prediction.dto;

import java.util.List;
import java.util.UUID;

public record PredictionResponse(
    UUID id,
    UUID userId,
    String raceId,
    String sessionType,
    boolean locked,
    List<String> rankedDriverCodes
) {}
