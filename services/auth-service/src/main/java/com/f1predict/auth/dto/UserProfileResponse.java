package com.f1predict.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String email,
    String username,
    boolean emailVerified,
    Instant createdAt
) {}
