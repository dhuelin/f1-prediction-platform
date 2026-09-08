package com.f1predict;

import com.f1predict.league.api.LeagueDirectory;
import com.f1predict.prediction.api.PredictionDirectory;
import com.f1predict.scoring.api.ScoringDirectory;
import com.f1predict.scoring.internal.LeagueLookup;
import com.f1predict.scoring.internal.PredictionLookup;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the consolidation of six services into one application.
 *
 * Merging six Spring Boot apps means six component scans, six sets of
 * @Configuration classes and six Flyway histories share one context. Two
 * startup-fatal collisions showed up that way and neither was visible to any
 * unit test: three classes named GlobalExceptionHandler, and two named
 * F1DataClient — component scanning derives bean names from simple class names,
 * so each pair aborted startup with ConflictingBeanDefinitionException.
 *
 * This test fails loudly on the next such collision instead of leaving it to be
 * discovered by a deploy.
 */
@SpringBootTest
@Testcontainers
class PitwallApiContextTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", () -> "localhost");
        registry.add("spring.rabbitmq.port", () -> "5672");
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
        registry.add("spring.rabbitmq.dynamic", () -> "false");
    }

    @MockBean RabbitTemplate rabbitTemplate;

    @Autowired PredictionDirectory predictionDirectory;
    @Autowired LeagueDirectory leagueDirectory;
    @Autowired ScoringDirectory scoringDirectory;
    @Autowired PredictionLookup predictionLookup;
    @Autowired LeagueLookup leagueLookup;
    @Autowired JdbcTemplate jdbc;

    @Test
    void allSixDomainsShareOneContext() {
        // The in-process contracts that replaced the HTTP hops between services
        assertThat(predictionDirectory).isNotNull();
        assertThat(leagueDirectory).isNotNull();
        assertThat(scoringDirectory).isNotNull();
        assertThat(predictionLookup).isNotNull();
        assertThat(leagueLookup).isNotNull();
    }

    @Test
    void allMigrationsApplyToTheSingleDatabase() {
        Integer applied = jdbc.queryForObject(
            "select count(*) from flyway_schema_history where success", Integer.class);
        assertThat(applied).isEqualTo(11);

        // Every domain's tables now live in one schema; JPA runs with
        // ddl-auto=validate, so booting at all proves the entities still match.
        List<String> tables = jdbc.queryForList(
            "select table_name from information_schema.tables where table_schema = 'public'",
            String.class);
        assertThat(tables).contains(
            "users", "refresh_tokens", "oauth_accounts",
            "leagues", "league_members", "scoring_configs",
            "predictions", "prediction_entries", "bonus_bets",
            "race_scores", "league_standings",
            "device_tokens", "notification_preferences",
            "race_analytics_events", "prediction_participation_stats");
    }
}
