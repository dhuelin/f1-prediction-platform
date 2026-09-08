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
public class GoogleTokenVerifier {
    private final DefaultJWTProcessor<SecurityContext> processor;
    private final String expectedClientId;

    public GoogleTokenVerifier(@Value("${oauth2.google.client-id}") String clientId) throws Exception {
        this.expectedClientId = clientId;
        JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(
            new URL("https://www.googleapis.com/oauth2/v3/certs"));
        var keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
        processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(keySelector);
    }

    public GoogleClaims verify(String idToken) throws Exception {
        JWTClaimsSet claims = processor.process(idToken, null);
        if (!claims.getAudience().contains(expectedClientId)) {
            throw new IllegalArgumentException("Token audience mismatch");
        }
        return new GoogleClaims(
            claims.getSubject(),
            (String) claims.getClaim("email"),
            (String) claims.getClaim("name")
        );
    }

    public record GoogleClaims(String subject, String email, String name) {}
}
