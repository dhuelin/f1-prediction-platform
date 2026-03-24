package com.f1predict.auth.dto;

import jakarta.validation.constraints.*;

public record ResetPasswordRequest(
    @NotBlank String token,
    @NotBlank @Size(min = 8) String newPassword
) {}
