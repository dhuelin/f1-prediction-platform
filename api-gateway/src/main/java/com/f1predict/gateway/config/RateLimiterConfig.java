package com.f1predict.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    static final String USER_ID_HEADER = "X-User-Id";
    static final String CF_CONNECTING_IP_HEADER = "CF-Connecting-IP";
    static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

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
     * Rate-limit key: the calling client, as well as it can be established.
     *
     * Used by /auth/**, where there is no authenticated user yet, so the user key
     * above is not available and some property of the caller has to stand in.
     *
     * Order of preference:
     *   1. CF-Connecting-IP -- set by Cloudflare, which proxies the apex and www,
     *      and passed through by nginx since nginx only overrides the headers it
     *      is told to.
     *   2. The left-most entry of X-Forwarded-For. nginx builds this with
     *      $proxy_add_x_forwarded_for, which appends its own $remote_addr to
     *      whatever arrived, so the original client is first and Cloudflare's edge
     *      address is after it.
     *   3. The socket peer address, which behind nginx is 127.0.0.1.
     *
     * Why this is worth doing even though a header can be forged: the limit used
     * to key on the socket address alone, which behind nginx is 127.0.0.1 for
     * every request. That made /auth/** a single global bucket of 5/s shared by
     * everyone, so any one client could exhaust it and lock every other user out
     * of logging in -- a denial of service against the whole site, reachable by
     * accident. Keying on the forwarded client address removes that: an honest
     * user gets their own bucket and cannot be starved by someone else.
     *
     * What it does not do: stop a determined attacker. Anyone who reaches the
     * origin directly -- port 80 is open and api.pitwall.guru is a DNS-only
     * record pointing at the VPS -- can set CF-Connecting-IP to whatever they
     * like and get a fresh bucket per request. Closing that means restricting
     * port 80 to Cloudflare's published ranges at the firewall or in nginx, which
     * is a change on the server rather than in this repository. Until then treat
     * this as removing a self-inflicted outage, not as brute-force protection; a
     * per-account throttle in pitwall-api would be the unforgeable version.
     */
    @Bean
    public KeyResolver clientIpKeyResolver() {
        return exchange -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();

            String cloudflare = headers.getFirst(CF_CONNECTING_IP_HEADER);
            if (cloudflare != null && !cloudflare.isBlank()) {
                return Mono.just(cloudflare.trim());
            }

            String forwardedFor = headers.getFirst(FORWARDED_FOR_HEADER);
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                // Left-most is the original client; the rest are the proxies it
                // passed through. A trailing comma or an empty first element
                // would otherwise become a shared bucket named "".
                for (String hop : forwardedFor.split(",")) {
                    if (!hop.isBlank()) {
                        return Mono.just(hop.trim());
                    }
                }
            }

            return Mono.just(remoteAddress(exchange));
        };
    }

    /**
     * Rate-limit key: the socket peer address.
     *
     * No route uses this any more -- /auth/** moved to clientIpKeyResolver, for
     * the reasons in its javadoc. It stays as the unambiguous default below.
     *
     * Marked @Primary because Spring Cloud Gateway's own
     * requestRateLimiterGatewayFilterFactory autowires a single KeyResolver as the
     * default for routes that do not name one. With two KeyResolver beans and no
     * primary, that injection point is ambiguous and the whole gateway context
     * fails to refresh. Every route here names its resolver explicitly, so this
     * default is only a fallback -- but it has to be unambiguous.
     */
    @Bean
    @Primary
    public KeyResolver remoteAddrKeyResolver() {
        return exchange -> Mono.just(remoteAddress(exchange));
    }

    private static String remoteAddress(org.springframework.web.server.ServerWebExchange exchange) {
        return exchange.getRequest().getRemoteAddress() != null
            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }
}
