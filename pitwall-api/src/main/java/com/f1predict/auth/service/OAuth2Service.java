package com.f1predict.auth.service;

import com.f1predict.auth.dto.AuthResponse;
import com.f1predict.auth.model.OAuthAccount;
import com.f1predict.auth.model.User;
import com.f1predict.auth.repository.OAuthAccountRepository;
import com.f1predict.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuth2Service {
    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final AuthService authService;

    public OAuth2Service(UserRepository userRepository,
                         OAuthAccountRepository oauthAccountRepository,
                         AuthService authService) {
        this.userRepository = userRepository;
        this.oauthAccountRepository = oauthAccountRepository;
        this.authService = authService;
    }

    @Transactional
    public AuthResponse handleOAuth(String email, String providerSubject,
                                    OAuthAccount.OAuthProvider provider, String name) {
        return oauthAccountRepository
            .findByProviderAndProviderSubject(provider, providerSubject)
            .map(oauthAccount -> authService.issueTokens(oauthAccount.getUser()))
            .orElseGet(() -> {
                // On first login email is always present; on repeat logins OAuthAccount already exists
                // Guard defensively in case email is null
                User user;
                if (email != null) {
                    user = userRepository.findByEmail(email)
                        .orElseGet(() -> {
                            User newUser = new User();
                            newUser.setEmail(email);
                            newUser.setUsername(generateUsername(name));
                            newUser.setEmailVerified(true);
                            return userRepository.save(newUser);
                        });
                } else {
                    // No email — create a user without email (should not normally happen on first Apple login)
                    User newUser = new User();
                    newUser.setUsername(generateUsername(name));
                    newUser.setEmailVerified(true);
                    user = userRepository.save(newUser);
                }
                OAuthAccount link = new OAuthAccount();
                link.setUser(user);
                link.setProvider(provider);
                link.setProviderSubject(providerSubject);
                oauthAccountRepository.save(link);
                return authService.issueTokens(user);
            });
    }

    private String generateUsername(String name) {
        String base = (name != null ? name : "user").toLowerCase().replaceAll("[^a-z0-9]", "");
        if (base.isEmpty()) base = "user";
        return base + "_" + (int)(Math.random() * 9999);
    }
}
