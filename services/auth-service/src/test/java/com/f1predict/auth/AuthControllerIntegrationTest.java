package com.f1predict.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.f1predict.auth.repository.PasswordResetTokenRepository;
import com.f1predict.auth.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordResetTokenRepository passwordResetTokenRepository;

    @Test
    void register_withValidRequest_returns201WithTokens() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"test@example.com","username":"testuser","password":"securepass123"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        String body = """
            {"email":"dupe@example.com","username":"user1","password":"securepass123"}
            """;
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"dupe@example.com","username":"user2","password":"securepass123"}
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void login_withValidCredentials_returns200WithTokens() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"login@example.com","username":"loginuser","password":"securepass123"}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"login@example.com","password":"securepass123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"nobody@example.com","password":"wrongpass"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withValidToken_returnsNewAccessToken() throws Exception {
        String response = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"refresh@example.com","username":"refreshuser","password":"securepass123"}
                    """))
            .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String refreshToken = mapper.readTree(response).get("refreshToken").asText();

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void forgotPassword_withExistingEmail_returns200() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"reset@example.com\",\"username\":\"resetuser\",\"password\":\"securepass123\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"reset@example.com\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_withUnknownEmail_stillReturns200() throws Exception {
        mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"unknown-reset@example.com\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void resetPassword_withValidToken_updatesPassword() throws Exception {
        // Register user
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"pwreset@example.com\",\"username\":\"pwresetuser\",\"password\":\"oldpassword1\"}"))
            .andExpect(status().isCreated());

        // Insert a known reset token directly into DB (known raw value → known hash)
        String rawToken = "valid-reset-token-for-test-abc123";
        String tokenHash = sha256Hex(rawToken);
        com.f1predict.auth.model.User user = userRepository.findByEmail("pwreset@example.com").orElseThrow();
        com.f1predict.auth.model.PasswordResetToken resetToken = new com.f1predict.auth.model.PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(java.time.Instant.now().plusSeconds(900));
        passwordResetTokenRepository.save(resetToken);

        // Call reset-password with the known raw token
        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"newpassword1\"}"))
            .andExpect(status().isOk());

        // Verify token is now marked used
        com.f1predict.auth.model.PasswordResetToken used =
            passwordResetTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertThat(used.isUsed()).isTrue();

        // Verify new password works (login succeeds)
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"pwreset@example.com\",\"password\":\"newpassword1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void resetPassword_withExpiredToken_returns401() throws Exception {
        // Create a user directly in DB
        com.f1predict.auth.model.User u = new com.f1predict.auth.model.User();
        u.setEmail("pwreset2@example.com");
        u.setUsername("pwreset2user");
        u.setPasswordHash("x");
        com.f1predict.auth.model.User savedUser = userRepository.save(u);

        String rawToken = "expired-test-token-123";
        String tokenHash = sha256Hex(rawToken);

        com.f1predict.auth.model.PasswordResetToken expired = new com.f1predict.auth.model.PasswordResetToken();
        expired.setUser(savedUser);
        expired.setTokenHash(tokenHash);
        expired.setExpiresAt(java.time.Instant.now().minusSeconds(1));
        passwordResetTokenRepository.save(expired);

        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"newpassword1\"}"))
            .andExpect(status().isUnauthorized());
    }

    private String sha256Hex(String input) throws Exception {
        var digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
