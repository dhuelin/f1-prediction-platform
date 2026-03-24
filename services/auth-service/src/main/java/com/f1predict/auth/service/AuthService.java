package com.f1predict.auth.service;

import com.f1predict.auth.dto.AuthResponse;
import com.f1predict.auth.dto.ForgotPasswordRequest;
import com.f1predict.auth.dto.LoginRequest;
import com.f1predict.auth.dto.RefreshRequest;
import com.f1predict.auth.dto.RegisterRequest;
import com.f1predict.auth.dto.ResetPasswordRequest;
import com.f1predict.auth.exception.EmailAlreadyExistsException;
import com.f1predict.auth.exception.InvalidCredentialsException;
import com.f1predict.auth.exception.InvalidTokenException;
import com.f1predict.auth.exception.UsernameAlreadyExistsException;
import com.f1predict.auth.model.PasswordResetToken;
import com.f1predict.auth.model.RefreshToken;
import com.f1predict.auth.model.User;
import com.f1predict.auth.repository.PasswordResetTokenRepository;
import com.f1predict.auth.repository.RefreshTokenRepository;
import com.f1predict.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        RefreshTokenRepository refreshTokenRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
        @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }
        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);
        String rawRefresh = generateRawRefreshToken();
        persistRefreshToken(saved, rawRefresh);
        return new AuthResponse(jwtService.generateAccessToken(saved), rawRefresh, accessTokenExpiry);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        refreshTokenRepository.deleteAllByUserId(user.getId());  // invalidate prior sessions
        String rawRefresh = generateRawRefreshToken();
        persistRefreshToken(user, rawRefresh);
        return new AuthResponse(jwtService.generateAccessToken(user), rawRefresh, accessTokenExpiry);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = hashToken(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(InvalidTokenException::new);
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException();
        }
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        String newRefreshRaw = generateRawRefreshToken();
        persistRefreshToken(user, newRefreshRaw);
        return new AuthResponse(jwtService.generateAccessToken(user), newRefreshRaw, accessTokenExpiry);
    }

    private String generateRawRefreshToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void persistRefreshToken(User user, String rawRefreshToken) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hashToken(rawRefreshToken));
        token.setExpiresAt(Instant.now().plusSeconds(refreshTokenExpiry));
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            passwordResetTokenRepository.deleteAllByUserId(user.getId());  // invalidate prior tokens
            String rawToken = UUID.randomUUID().toString();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(hashToken(rawToken));
            token.setExpiresAt(Instant.now().plusSeconds(900));
            passwordResetTokenRepository.save(token);
            log.debug("Password reset token for user id {}: {}", user.getId(), rawToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hash = hashToken(request.token());
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash)
            .orElseThrow(InvalidTokenException::new);
        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException();
        }
        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private String hashToken(String raw) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
