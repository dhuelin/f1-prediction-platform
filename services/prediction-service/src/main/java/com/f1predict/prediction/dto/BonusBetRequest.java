package com.f1predict.prediction.dto;

import com.f1predict.prediction.model.BetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BonusBetRequest(
    @NotNull BetType betType,
    @NotNull @Min(1) Integer stake,
    @NotBlank String betValue
) {}
