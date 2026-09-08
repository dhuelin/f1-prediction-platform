package com.f1predict.auth.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    long expiresIn
) {}
