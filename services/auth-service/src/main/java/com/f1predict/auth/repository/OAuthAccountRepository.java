package com.f1predict.auth.repository;

import com.f1predict.auth.model.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {
    Optional<OAuthAccount> findByProviderAndProviderSubject(
            OAuthAccount.OAuthProvider provider, String providerSubject);
}
