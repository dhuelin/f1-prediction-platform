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
 * on its own is one global bucket.
 *
 * Also covers clientIpKeyResolver, which /auth/** uses: there is no
 * authenticated user at login time, so the forwarded client address stands in.
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

    // ── clientIpKeyResolver, used by /auth/** ──────────────────────────────

    @Test
    void prefersTheCloudflareClientAddress() {
        KeyResolver resolver = config.clientIpKeyResolver();
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/auth/login")
                .header("CF-Connecting-IP", "203.0.113.7")
                .header("X-Forwarded-For", "198.51.100.4, 172.16.0.1"));

        // Cloudflare proxies the apex, so its header is the most direct statement
        // of who is calling; X-Forwarded-For is only consulted without it.
        assertThat(resolver.resolve(exchange).block()).isEqualTo("203.0.113.7");
    }

    @Test
    void fallsBackToTheLeftMostForwardedForEntry() {
        KeyResolver resolver = config.clientIpKeyResolver();
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/auth/login")
                .header("X-Forwarded-For", "198.51.100.4, 172.16.0.1, 127.0.0.1"));

        // nginx appends with $proxy_add_x_forwarded_for, so the original client
        // is first and every later hop is a proxy.
        assertThat(resolver.resolve(exchange).block()).isEqualTo("198.51.100.4");
    }

    @Test
    void differentClientsGetDifferentBuckets() {
        KeyResolver resolver = config.clientIpKeyResolver();

        String a = resolver.resolve(MockServerWebExchange.from(
            MockServerHttpRequest.post("/auth/login")
                .header("CF-Connecting-IP", "203.0.113.7"))).block();
        String b = resolver.resolve(MockServerWebExchange.from(
            MockServerHttpRequest.post("/auth/login")
                .header("CF-Connecting-IP", "203.0.113.8"))).block();

        // The point of the change. Keying on the socket peer gave both of these
        // "127.0.0.1", so one client exhausting the bucket locked the other out
        // of logging in.
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void noForwardedHeadersFallsBackToTheSocketAddress() {
        KeyResolver resolver = config.clientIpKeyResolver();
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/auth/login").remoteAddress(
                new java.net.InetSocketAddress("127.0.0.1", 12345)));

        assertThat(resolver.resolve(exchange).block()).isEqualTo("127.0.0.1");
    }

    @Test
    void blankAndMalformedHeadersDoNotBecomeASharedBucket() {
        KeyResolver resolver = config.clientIpKeyResolver();

        // A blank CF header must not win over a usable X-Forwarded-For.
        String blankCf = resolver.resolve(MockServerWebExchange.from(
            MockServerHttpRequest.post("/auth/login")
                .header("CF-Connecting-IP", "   ")
                .header("X-Forwarded-For", "198.51.100.4"))).block();
        assertThat(blankCf).isEqualTo("198.51.100.4");

        // A leading comma must not resolve to the empty string, which would be
        // one bucket shared by every caller that sent one.
        String leadingComma = resolver.resolve(MockServerWebExchange.from(
            MockServerHttpRequest.post("/auth/login")
                .header("X-Forwarded-For", ", 198.51.100.9"))).block();
        assertThat(leadingComma).isEqualTo("198.51.100.9");

        // Entirely unusable headers fall through to the socket address.
        String allBlank = resolver.resolve(MockServerWebExchange.from(
            MockServerHttpRequest.post("/auth/login")
                .header("X-Forwarded-For", " , , ")
                .remoteAddress(new java.net.InetSocketAddress("127.0.0.1", 12345)))).block();
        assertThat(allBlank).isEqualTo("127.0.0.1");
    }
}
