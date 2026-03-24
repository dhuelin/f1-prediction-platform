package com.f1predict.auth.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URL;

@Component
public class AppleTokenVerifier {
    private final DefaultJWTProcessor<SecurityContext> processor;
    private final String expectedClientId;

    public AppleTokenVerifier(@Value("${oauth2.apple.client-id}") String clientId) throws Exception {
        this.expectedClientId = clientId;
        JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(
            new URL("https://appleid.apple.com/auth/keys"));
        var keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, jwkSource);
        processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(keySelector);
    }

    public AppleClaims verify(String idToken) throws Exception {
        JWTClaimsSet claims = processor.process(idToken, null);
        if (!claims.getAudience().contains(expectedClientId)) {
            throw new IllegalArgumentException("Token audience mismatch");
        }
        String email = (String) claims.getClaim("email");
        String name = (String) claims.getClaim("name");
        return new AppleClaims(claims.getSubject(), email, name);
    }

    public record AppleClaims(String subject, String email, String name) {}
}
