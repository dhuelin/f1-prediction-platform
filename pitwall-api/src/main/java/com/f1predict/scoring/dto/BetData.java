package com.f1predict.scoring.dto;

public record BetData(
    String betType,   // FASTEST_LAP | DNF_DSQ_DNS | SC_DEPLOYED | SC_COUNT
    int stake,
    String betValue   // driver code, "true"/"false", or count as string
) {}
