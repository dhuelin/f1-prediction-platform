package com.f1predict.notification.controller;

import com.f1predict.notification.model.DeviceToken;
import com.f1predict.notification.model.NotificationPreferences;
import com.f1predict.notification.repository.DeviceTokenRepository;
import com.f1predict.notification.repository.NotificationPreferencesRepository;
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
    private final NotificationPreferencesRepository preferencesRepository;

    public NotificationController(DeviceTokenRepository tokenRepository,
                                  NotificationPreferencesRepository preferencesRepository) {
        this.tokenRepository = tokenRepository;
        this.preferencesRepository = preferencesRepository;
    }

    record RegisterTokenRequest(
        @NotBlank String token,
        @NotBlank @Pattern(regexp = "APNS|FCM") String platform
    ) {}

    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerToken(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody RegisterTokenRequest req) {
        tokenRepository.findByUserIdAndToken(userId, req.token()).ifPresentOrElse(
            existing -> {},
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

    record PreferencesResponse(boolean predictionReminder, boolean raceStart,
                               boolean resultsPublished, boolean scoreAmended) {}

    record UpdatePreferencesRequest(boolean predictionReminder, boolean raceStart,
                                    boolean resultsPublished, boolean scoreAmended) {}

    @GetMapping("/preferences")
    public PreferencesResponse getPreferences(@RequestHeader("X-User-Id") UUID userId) {
        var prefs = preferencesRepository.findByUserId(userId)
            .orElse(new NotificationPreferences(userId));
        return new PreferencesResponse(
            prefs.isPredictionReminder(), prefs.isRaceStart(),
            prefs.isResultsPublished(), prefs.isScoreAmended());
    }

    @PutMapping("/preferences")
    public PreferencesResponse updatePreferences(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody UpdatePreferencesRequest req) {
        var prefs = preferencesRepository.findByUserId(userId)
            .orElseGet(() -> new NotificationPreferences(userId));
        prefs.update(req.predictionReminder(), req.raceStart(),
                     req.resultsPublished(), req.scoreAmended());
        preferencesRepository.save(prefs);
        return new PreferencesResponse(
            prefs.isPredictionReminder(), prefs.isRaceStart(),
            prefs.isResultsPublished(), prefs.isScoreAmended());
    }
}
