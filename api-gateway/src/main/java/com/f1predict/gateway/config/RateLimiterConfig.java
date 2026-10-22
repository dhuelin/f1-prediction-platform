package com.f1predict.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    static final String USER_ID_HEADER = "X-User-Id";

    /**
     * Rate-limit key: the authenticated user.
     *
     * Preferred over the IP for any route behind a protected prefix. X-User-Id is
     * written by {@link JwtGatewayFilter} from a verified JWT signature, and that
     * filter is a GlobalFilter ordered -1, so it has already run by the time a
     * route's RequestRateLimiter does. The value cannot be spoofed: the filter
     * removes any client-supplied copy before adding its own.
     *
     * An IP key would be the wrong unit here. The gateway sits behind nginx on
     * localhost, so every request's remote address is 127.0.0.1 (see
     * remoteAddrKeyResolver), and the forwarded headers that do carry a client IP
     * are settable by anyone who reaches the origin directly — which is possible
     * today, since api.pitwall.guru is a DNS-only record and port 80 is open.
     *
     * Falls back to the remote address if the header is somehow absent, so a
     * misconfiguration degrades to a shared bucket rather than to no limit at all.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }
            return Mono.just(remoteAddress(exchange));
        };
    }

    /**
     * Rate-limit key: the client's remote IP address.
     * Used by the RequestRateLimiter filter on /auth/** routes to throttle brute-force attempts.
     *
     * NOTE: behind the production nginx this resolves to 127.0.0.1 for every
     * request, so the /auth/** limit currently behaves as a single global bucket
     * rather than per-client. Fixing that means deciding which forwarded header to
     * trust and whether the origin may be reached without going through Cloudflare;
     * see the README.
     */
    @Bean
    public KeyResolver remoteAddrKeyResolver() {
        return exchange -> Mono.just(remoteAddress(exchange));
    }

    private static String remoteAddress(org.springframework.web.server.ServerWebExchange exchange) {
        return exchange.getRequest().getRemoteAddress() != null
            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }
}
