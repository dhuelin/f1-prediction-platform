package com.f1predict.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthCallbackRequest(@NotBlank String idToken) {}
