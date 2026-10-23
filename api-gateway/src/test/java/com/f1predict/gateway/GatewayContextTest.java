package com.f1predict.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

/**
 * Boots the whole gateway context.
 *
 * The unit tests around routes and key resolvers all construct their subjects
 * directly, so they cannot see a wiring fault. This test can: declaring a second
 * KeyResolver bean without a primary broke the gateway's own
 * requestRateLimiterGatewayFilterFactory, which autowires a single one, and the
 * context only failed at container start.
 *
 * No Redis is needed -- Lettuce connects lazily, and nothing here sends a command.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.data.redis.host=localhost",
    "jwt.secret=test-secret-that-is-long-enough-for-hs256-signing-key"
})
class GatewayContextTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    void everyKeyResolverInjectionPointCanBeResolved() {
        // More than one KeyResolver is fine, but exactly one must be primary,
        // otherwise GatewayAutoConfiguration's single-KeyResolver parameter fails.
        assertThat(context.getBeanNamesForType(KeyResolver.class)).hasSizeGreaterThan(1);
        assertThat(context.getBean(KeyResolver.class)).isNotNull();
    }

    @Test
    void allConfiguredRoutesAreBuilt() {
        List<String> ids = routeLocator.getRoutes()
            .map(route -> route.getId())
            .collectList()
            .block();

        assertThat(ids).containsExactly(
            "pitwall-api-auth",
            "f1-data-service",
            "pitwall-api-league-join",
            "pitwall-api",
            "f1-data-service-ws",
            "web-app",
            "web-static"
        );
    }
}
