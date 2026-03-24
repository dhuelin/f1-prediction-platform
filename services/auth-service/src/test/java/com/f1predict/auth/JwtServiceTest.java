package com.f1predict.auth;

import com.f1predict.auth.model.User;
import com.f1predict.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    JwtService jwtService = new JwtService(
        "test-secret-key-that-is-at-least-256-bits-long-for-testing",
        900L,
        2592000L
    );

    @Test
    void generateAccessToken_containsUserId() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId().toString());
    }

    @Test
    void validateToken_withExpiredToken_returnsFalse() {
        JwtService shortLivedService = new JwtService(
            "test-secret-key-that-is-at-least-256-bits-long-for-testing", -1L, 2592000L);
        User user = new User();
        user.setId(UUID.randomUUID());
        String token = shortLivedService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void validateToken_withValidToken_returnsTrue() {
        User user = new User();
        user.setId(UUID.randomUUID());
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }
}
