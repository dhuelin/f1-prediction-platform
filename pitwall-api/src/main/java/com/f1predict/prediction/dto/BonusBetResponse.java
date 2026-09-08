package com.f1predict.prediction.dto;

import com.f1predict.prediction.model.BetType;
import java.util.UUID;

public record BonusBetResponse(
    UUID id,
    BetType betType,
    int stake,
    String betValue
) {}
