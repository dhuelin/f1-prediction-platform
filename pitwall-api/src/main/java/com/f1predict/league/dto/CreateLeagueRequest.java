package com.f1predict.league.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLeagueRequest(
    @NotBlank @Size(min = 3, max = 100) String name,
    String visibility  // "PUBLIC" or "PRIVATE", defaults to "PUBLIC"
) {}
