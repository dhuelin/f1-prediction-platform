package com.f1predict.scoring.dto;

import java.util.UUID;

public record StandingEntry(UUID userId, int totalPoints, int rank) {}
