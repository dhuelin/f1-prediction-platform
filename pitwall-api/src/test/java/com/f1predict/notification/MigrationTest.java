package com.f1predict.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class MigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.rabbitmq.host", () -> "localhost");
        r.add("spring.rabbitmq.port", () -> "5672");
        r.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
    }

    // Spring AMQP uses lazy connections by default in Spring Boot 3, so no real broker is needed.
    // Mocking RabbitTemplate prevents any accidental publish attempts during the test.
    @MockBean org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Autowired JdbcTemplate jdbc;

    @Test
    void deviceTokensTableExists() {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'device_tokens' AND table_schema = 'public'",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void notificationPreferencesTableExists() {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'notification_preferences' AND table_schema = 'public'",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
