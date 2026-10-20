package com.f1predict.league.dto;

import java.time.Instant;
import java.util.UUID;

public record LeagueMemberResponse(
    UUID userId,
    String username,
    boolean isAdmin,
    Instant joinedAt
) {}
