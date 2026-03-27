package com.f1predict.prediction.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BonusBetRequest(
    @NotBlank String betType,    // FASTEST_LAP | DNF_DSQ_DNS | SC_DEPLOYED | SC_COUNT
    @NotNull @Min(1) Integer stake,
    @NotBlank String betValue    // driver code, "true"/"false", or count as string
) {}
