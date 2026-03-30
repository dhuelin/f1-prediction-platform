package com.f1predict.notification.controller;

import com.f1predict.notification.model.DeviceToken;
import com.f1predict.notification.repository.DeviceTokenRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final DeviceTokenRepository tokenRepository;

    public NotificationController(DeviceTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    record RegisterTokenRequest(
        @NotBlank String token,
        @Pattern(regexp = "APNS|FCM") String platform
    ) {}

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerToken(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody RegisterTokenRequest req) {
        // Idempotent: only insert if not already present
        tokenRepository.findByUserIdAndToken(userId, req.token()).ifPresentOrElse(
            existing -> {}, // already exists, no-op
            () -> tokenRepository.save(
                new DeviceToken(userId, req.token(),
                    DeviceToken.Platform.valueOf(req.platform())))
        );
    }

    @DeleteMapping("/devices/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeToken(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable String token) {
        tokenRepository.findByUserIdAndToken(userId, token)
            .ifPresent(tokenRepository::delete);
    }
}
