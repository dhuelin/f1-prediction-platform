package com.f1predict.auth.repository;

import com.f1predict.auth.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user.id = :userId")
    void deleteAllByUserId(UUID userId);
}
