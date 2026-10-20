package com.f1predict.gateway;

import com.f1predict.gateway.config.JwtGatewayFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtGatewayFilterTest {

    private static final String TEST_SECRET = "test-secret-key-32-chars-minimum!";
    private JwtGatewayFilter filter;
    private SecretKey testKey;

    @BeforeEach
    void setUp() {
        filter = new JwtGatewayFilter(TEST_SECRET);
        testKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void publicAuthPath_withoutToken_passesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/auth/login")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> {
            // If chain is called, the request passed through
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void protectedPath_withoutAuthHeader_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/f1/races")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedPath_withInvalidJwt_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/f1/races")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedPath_withValidJwt_passesThrough_andForwardsUserId() {
        String token = Jwts.builder()
                .subject("user-123")
                .signWith(testKey)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/f1/races")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Capture the mutated exchange passed to the chain
        String[] capturedUserId = {null};
        GatewayFilterChain chain = ex -> {
            capturedUserId[0] = ex.getRequest().getHeaders().getFirst("X-User-Id");
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(capturedUserId[0]).isEqualTo("user-123");
    }

    @Test
    void protectedPath_withJwtMissingSubject_returns401() {
        String token = Jwts.builder()
                .signWith(testKey)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/f1/races")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // WebSocket upgrade requests cannot carry Authorization headers, so the JWT
    // is passed as a ?token= query parameter instead.

    @Test
    void wsPath_withValidTokenQueryParam_passesThrough_andForwardsUserId() {
        String token = Jwts.builder()
                .subject("user-ws-1")
                .signWith(testKey)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/ws/live/race-42?token=" + token)
                .header("Upgrade", "websocket")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String[] capturedUserId = {null};
        GatewayFilterChain chain = ex -> {
            capturedUserId[0] = ex.getRequest().getHeaders().getFirst("X-User-Id");
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(capturedUserId[0]).isEqualTo("user-ws-1");
    }

    @Test
    void wsPath_withoutToken_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/ws/live/race-42")
                .header("Upgrade", "websocket")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void wsPath_withInvalidTokenQueryParam_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/ws/live/race-42?token=invalid.jwt.token")
                .header("Upgrade", "websocket")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> Mono.empty();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── Collection roots ─────────────────────────────────────────────────────
    // PROTECTED_PREFIXES used to carry trailing slashes and be matched with a bare
    // startsWith, so "/leagues" did not match "/leagues/" and the collection root
    // was unauthenticated. The unprotected branch returns before X-User-Id is
    // stripped, so a client could also name whichever user it liked.

    @Test
    void collectionRoot_withoutToken_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/leagues")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> {
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void collectionRoot_withForgedUserIdAndNoToken_returns401() {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/leagues")
                .header("X-User-Id", "11111111-1111-1111-1111-111111111111")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String[] capturedUserId = {null};
        GatewayFilterChain chain = ex -> {
            capturedUserId[0] = ex.getRequest().getHeaders().getFirst("X-User-Id");
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // The request must never reach the backend carrying the forged identity.
        assertThat(capturedUserId[0]).isNull();
    }

    @Test
    void collectionRoot_withValidJwt_forwardsVerifiedUserIdOverForgedHeader() {
        String token = Jwts.builder()
                .subject("user-123")
                .signWith(testKey)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest
                .post("/leagues")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-User-Id", "11111111-1111-1111-1111-111111111111")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String[] capturedUserIds = {null};
        GatewayFilterChain chain = ex -> {
            capturedUserIds[0] = String.join(",", ex.getRequest().getHeaders().get("X-User-Id"));
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
        // Exactly one value, from the JWT — not the client's, and not both.
        assertThat(capturedUserIds[0]).isEqualTo("user-123");
    }

    @Test
    void pathMerelySharingAPrefix_isNotTreatedAsProtected() {
        // Matching must respect the segment boundary: /leaguesomething is not /leagues
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/leaguesomething")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> {
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void subPathsRemainProtected() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/leagues/mine")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> {
            ex.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
