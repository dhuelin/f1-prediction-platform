package com.f1predict.prediction.dto;

import java.util.UUID;

public record BonusBetResponse(
    UUID id,
    String betType,
    int stake,
    String betValue
) {}
