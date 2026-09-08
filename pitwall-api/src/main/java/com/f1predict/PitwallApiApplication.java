package com.f1predict;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single entry point for the Pitwall API.
 *
 * Consolidates what used to be six separate Spring Boot services — auth, league,
 * prediction, scoring, notification and analytics — into one deployable. Each
 * domain keeps its own package under com.f1predict.*; only the process boundary
 * between them is gone.
 *
 * f1-data-service stays a separate deployable: it owns the scheduled OpenF1
 * pollers and the live-race WebSocket broadcast, and talks to this app over
 * RabbitMQ.
 */
@SpringBootApplication
public class PitwallApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(PitwallApiApplication.class, args);
    }
}
