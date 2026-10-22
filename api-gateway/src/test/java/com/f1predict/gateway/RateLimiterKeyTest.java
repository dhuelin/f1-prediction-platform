package com.f1predict.gateway;

import com.f1predict.gateway.config.RateLimiterConfig;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the rate-limit key used by POST /leagues/join.
 *
 * The key must be the authenticated user, not the IP. The gateway runs behind
 * nginx on localhost, so every request's remote address is 127.0.0.1 — an IP key
 * is one global bucket, which is what the pre-existing /auth/** limit does.
 */
class RateLimiterKeyTest {

    private final RateLimiterConfig config = new RateLimiterConfig();

    @Test
    void keysOnTheVerifiedUserId() {
        KeyResolver resolver = config.userKeyResolver();
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/leagues/join").header("X-User-Id", "user-123"));

        assertThat(resolver.resolve(exchange).block()).isEqualTo("user-123");
    }

    @Test
    void differentUsersGetDifferentBuckets() {
        KeyResolver resolver = config.userKeyResolver();

        String a = resolver.resolve(MockServerWebExchange.from(
            MockServerHttpRequest.post("/leagues/join").header("X-User-Id", "user-a"))).block();
        String b = resolver.resolve(MockServerWebExchange.from(
            MockServerHttpRequest.post("/leagues/join").header("X-User-Id", "user-b"))).block();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void fallsBackToRemoteAddressWhenTheHeaderIsAbsent() {
        KeyResolver resolver = config.userKeyResolver();
        // Degrade to a shared bucket rather than to no limit at all.
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/leagues/join"));

        assertThat(resolver.resolve(exchange).block()).isNotNull();
    }

    @Test
    void blankHeaderIsTreatedAsAbsentRatherThanAsAKey() {
        KeyResolver resolver = config.userKeyResolver();
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/leagues/join").header("X-User-Id", "   "));

        // A blank key would put every such request in one bucket named "   ".
        assertThat(resolver.resolve(exchange).block()).isNotEqualTo("   ");
    }
}
