package com.f1predict.auth;

import com.f1predict.auth.model.RefreshToken;
import com.f1predict.auth.model.User;
import com.f1predict.auth.repository.EmailVerificationTokenRepository;
import com.f1predict.auth.repository.PasswordResetTokenRepository;
import com.f1predict.auth.repository.RefreshTokenRepository;
import com.f1predict.auth.repository.UserRepository;
import com.f1predict.auth.service.AuthService;
import com.f1predict.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers POST /auth/logout revoking the refresh token.
 *
 * Before this existed the endpoint 404'd and the client swallowed it, so a
 * refresh token stayed valid for its full 30 days after the user signed out.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthLogoutTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock EmailVerificationTokenRepository emailVerificationTokenRepository;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository, passwordEncoder, jwtService,
            refreshTokenRepository, passwordResetTokenRepository,
            emailVerificationTokenRepository,
            900L, 2592000L);
    }

    /** Mirrors AuthService's private hashToken (SHA-256, lower-case hex). */
    private static String sha256(String raw) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest(raw.getBytes(StandardCharsets.UTF_8))) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Test
    void revokesTheStoredToken() throws Exception {
        String raw = "a-refresh-token";
        RefreshToken stored = new RefreshToken();
        stored.setUser(new User());
        stored.setTokenHash(sha256(raw));
        stored.setExpiresAt(Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByTokenHash(sha256(raw))).thenReturn(Optional.of(stored));

        authService.logout(raw);

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void unknownTokenIsSilentlyIgnored() throws Exception {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        authService.logout("not-a-real-token");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void blankOrMissingTokenDoesNotTouchTheRepository() {
        authService.logout(null);
        authService.logout("");
        authService.logout("   ");

        verify(refreshTokenRepository, never()).findByTokenHash(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokingTwiceIsHarmless() throws Exception {
        String raw = "token";
        RefreshToken stored = new RefreshToken();
        stored.setUser(new User());
        stored.setTokenHash(sha256(raw));
        stored.setExpiresAt(Instant.now().plusSeconds(3600));
        stored.setRevoked(true);
        when(refreshTokenRepository.findByTokenHash(sha256(raw))).thenReturn(Optional.of(stored));

        authService.logout(raw);

        assertThat(stored.isRevoked()).isTrue();
    }
}
