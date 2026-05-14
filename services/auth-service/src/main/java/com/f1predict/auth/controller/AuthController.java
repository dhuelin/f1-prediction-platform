package com.f1predict.auth.controller;

import com.f1predict.auth.dto.AuthResponse;
import com.f1predict.auth.dto.ForgotPasswordRequest;
import com.f1predict.auth.dto.LoginRequest;
import com.f1predict.auth.dto.OAuthCallbackRequest;
import com.f1predict.auth.dto.RefreshRequest;
import com.f1predict.auth.dto.RegisterRequest;
import com.f1predict.auth.dto.ResetPasswordRequest;
import com.f1predict.auth.dto.UserProfileResponse;
import com.f1predict.auth.model.OAuthAccount;
import com.f1predict.auth.service.AppleTokenVerifier;
import com.f1predict.auth.service.AuthService;
import com.f1predict.auth.service.GoogleTokenVerifier;
import com.f1predict.auth.service.OAuth2Service;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AppleTokenVerifier appleTokenVerifier;
    private final OAuth2Service oauth2Service;

    public AuthController(AuthService authService,
                          GoogleTokenVerifier googleTokenVerifier,
                          AppleTokenVerifier appleTokenVerifier,
                          OAuth2Service oauth2Service) {
        this.authService = authService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.appleTokenVerifier = appleTokenVerifier;
        this.oauth2Service = oauth2Service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestParam String email) {
        authService.resendVerification(email);
        return ResponseEntity.ok().build();
    }

    // #161 — Current user profile (gateway injects X-User-Id from validated JWT)
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@RequestHeader("X-User-Id") UUID userId) {
        return authService.getProfile(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/oauth/google/callback")
    public AuthResponse googleCallback(@Valid @RequestBody OAuthCallbackRequest request) throws Exception {
        var claims = googleTokenVerifier.verify(request.idToken());
        return oauth2Service.handleOAuth(
            claims.email(), claims.subject(), OAuthAccount.OAuthProvider.GOOGLE, claims.name());
    }

    @PostMapping("/oauth/apple/callback")
    public AuthResponse appleCallback(@Valid @RequestBody OAuthCallbackRequest request) throws Exception {
        var claims = appleTokenVerifier.verify(request.idToken());
        return oauth2Service.handleOAuth(
            claims.email(), claims.subject(), OAuthAccount.OAuthProvider.APPLE,
            claims.name() != null ? claims.name() : "Apple User");
    }
}
