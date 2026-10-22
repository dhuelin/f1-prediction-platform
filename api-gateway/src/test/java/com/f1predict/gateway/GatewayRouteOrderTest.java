package com.f1predict.gateway;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the route ordering that makes the join rate limit effective.
 *
 * Spring Cloud Gateway takes the FIRST route whose predicates match. The general
 * pitwall-api route matches Path=/leagues/**, which includes /leagues/join, so if
 * it is ever declared before the dedicated join route the limit stops applying —
 * silently, with no error and no failing request.
 */
class GatewayRouteOrderTest {

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> routes() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> spring = (Map<String, Object>) root.get("spring");
            Map<String, Object> cloud = (Map<String, Object>) spring.get("cloud");
            Map<String, Object> gateway = (Map<String, Object>) cloud.get("gateway");
            return (List<Map<String, Object>>) gateway.get("routes");
        } catch (Exception e) {
            throw new IllegalStateException("could not read gateway routes", e);
        }
    }

    private int indexOf(String id) {
        List<Map<String, Object>> routes = routes();
        for (int i = 0; i < routes.size(); i++) {
            if (id.equals(routes.get(i).get("id"))) return i;
        }
        return -1;
    }

    @Test
    void joinRouteIsDeclaredBeforeTheGeneralLeaguesRoute() {
        int join = indexOf("pitwall-api-league-join");
        int general = indexOf("pitwall-api");

        assertThat(join).as("join route must exist").isNotNegative();
        assertThat(general).as("general route must exist").isNotNegative();
        assertThat(join)
            .as("the general route matches /leagues/** and would swallow /leagues/join")
            .isLessThan(general);
    }

    @Test
    @SuppressWarnings("unchecked")
    void joinRouteRateLimitsOnTheAuthenticatedUser() {
        Map<String, Object> route = routes().get(indexOf("pitwall-api-league-join"));

        List<Map<String, Object>> filters = (List<Map<String, Object>>) route.get("filters");
        assertThat(filters).as("join route must carry a rate limiter").isNotEmpty();

        Map<String, Object> limiter = filters.stream()
            .filter(f -> "RequestRateLimiter".equals(f.get("name")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no RequestRateLimiter on the join route"));

        Map<String, Object> args = (Map<String, Object>) limiter.get("args");
        // Keyed on the user: an IP key is one global bucket behind nginx.
        assertThat(args.get("key-resolver")).isEqualTo("#{@userKeyResolver}");
        assertThat(args.get("redis-rate-limiter.replenishRate")).isEqualTo(1);
        assertThat(args.get("redis-rate-limiter.requestedTokens")).isEqualTo(10);
        assertThat(args.get("redis-rate-limiter.burstCapacity")).isEqualTo(60);
    }

    @Test
    @SuppressWarnings("unchecked")
    void joinRouteMatchesOnlyThePostOnThatExactPath() {
        Map<String, Object> route = routes().get(indexOf("pitwall-api-league-join"));
        List<String> predicates = (List<String>) route.get("predicates");

        assertThat(predicates).contains("Path=/leagues/join", "Method=POST");
    }
}
