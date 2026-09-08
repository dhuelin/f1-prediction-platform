package com.f1predict.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The app's single RabbitMQ topology, merged from the four RabbitMQConfig classes
 * the auth/prediction/league/scoring/notification/analytics services used to carry.
 * Those four declared identically-named beans (f1Exchange, rabbitTemplate,
 * sessionCompleteQueue, ...), which would abort startup with a bean definition
 * conflict once they share a context.
 *
 * Scope is now inbound only: every event produced *inside* this app —
 * PredictionLockedEvent and StandingsUpdatedEvent — is a Spring ApplicationEvent
 * and never touches the broker. What remains is what genuinely crosses a process
 * boundary: events published by f1-data-service on the f1.events exchange.
 *
 * Each domain deliberately keeps its OWN queue per routing key rather than sharing
 * one. Three domains care about session.complete; on a single shared queue RabbitMQ
 * would round-robin deliveries between their listeners and each event would reach
 * only one of them. Separate queues also keep failure isolation as it was — a
 * notification handler that throws dead-letters only the notification copy.
 *
 * Queue names, arguments and dead-letter exchanges are preserved exactly as the
 * individual services declared them. RabbitMQ refuses to redeclare an existing
 * queue with different arguments, so changing the x-dead-letter-exchange values
 * here would break startup against the live broker.
 *
 * The prediction.events and scoring.events exchanges, and the four queues bound to
 * them, are gone from this config — they were only ever used for hops that are now
 * in-process. Any of those queues still sitting on the broker are unused and can be
 * deleted by hand.
 */
@Configuration
public class RabbitMQConfig {

    // Exchange owned by f1-data-service
    public static final String F1_EXCHANGE = "f1.events";

    // Routing keys published by f1-data-service
    public static final String SESSION_COMPLETE_KEY = "session.complete";
    public static final String RACE_RESULT_KEY      = "race.result.final";
    public static final String RESULT_AMENDED_KEY   = "race.result.amended";

    // Per-domain queues (names unchanged from the original services)
    public static final String PREDICTION_SESSION_COMPLETE_QUEUE   = "prediction-service.session-complete";
    public static final String SCORING_RACE_RESULT_QUEUE           = "scoring-service.race-result-final";
    public static final String SCORING_RESULT_AMENDED_QUEUE        = "scoring-service.result-amended";
    public static final String NOTIFICATION_SESSION_COMPLETE_QUEUE = "notification-service.session-complete";
    public static final String NOTIFICATION_RACE_RESULT_QUEUE      = "notification-service.race-result-final";
    public static final String NOTIFICATION_RESULT_AMENDED_QUEUE   = "notification-service.result-amended";
    public static final String ANALYTICS_SESSION_COMPLETE_QUEUE    = "analytics-service.session-complete";
    public static final String ANALYTICS_RACE_RESULT_QUEUE         = "analytics-service.race-result-final";

    // Dead-letter exchanges (names unchanged — see class javadoc)
    public static final String PREDICTION_DLX   = "prediction-service.dlx";
    public static final String SCORING_DLX      = "scoring-service.dlx";
    public static final String NOTIFICATION_DLX = "notification-service.dlx";
    public static final String ANALYTICS_DLX    = "analytics-service.dlx";

    @Bean
    public TopicExchange f1Exchange() {
        return new TopicExchange(F1_EXCHANGE, true, false);
    }

    @Bean public DirectExchange predictionDlx()   { return new DirectExchange(PREDICTION_DLX, true, false); }
    @Bean public DirectExchange scoringDlx()      { return new DirectExchange(SCORING_DLX, true, false); }
    @Bean public DirectExchange notificationDlx() { return new DirectExchange(NOTIFICATION_DLX, true, false); }
    @Bean public DirectExchange analyticsDlx()    { return new DirectExchange(ANALYTICS_DLX, true, false); }

    private static Queue durableQueue(String name, String dlx) {
        return QueueBuilder.durable(name)
            .withArgument("x-dead-letter-exchange", dlx)
            .withArgument("x-dead-letter-routing-key", name)
            .build();
    }

    // ── prediction domain ────────────────────────────────────────────────────
    @Bean public Queue predictionSessionCompleteQueue() {
        return durableQueue(PREDICTION_SESSION_COMPLETE_QUEUE, PREDICTION_DLX);
    }
    @Bean public Queue predictionSessionCompleteDlq() {
        return new Queue(PREDICTION_SESSION_COMPLETE_QUEUE + ".dlq", true);
    }
    @Bean public Binding predictionSessionCompleteBinding() {
        return BindingBuilder.bind(predictionSessionCompleteQueue()).to(f1Exchange()).with(SESSION_COMPLETE_KEY);
    }
    @Bean public Binding predictionSessionCompleteDlqBinding() {
        return BindingBuilder.bind(predictionSessionCompleteDlq()).to(predictionDlx()).with(PREDICTION_SESSION_COMPLETE_QUEUE);
    }

    // ── scoring domain ───────────────────────────────────────────────────────
    @Bean public Queue scoringRaceResultQueue() {
        return durableQueue(SCORING_RACE_RESULT_QUEUE, SCORING_DLX);
    }
    @Bean public Queue scoringResultAmendedQueue() {
        return durableQueue(SCORING_RESULT_AMENDED_QUEUE, SCORING_DLX);
    }
    @Bean public Queue scoringRaceResultDlq()    { return new Queue(SCORING_RACE_RESULT_QUEUE + ".dlq", true); }
    @Bean public Queue scoringResultAmendedDlq() { return new Queue(SCORING_RESULT_AMENDED_QUEUE + ".dlq", true); }
    @Bean public Binding scoringRaceResultBinding() {
        return BindingBuilder.bind(scoringRaceResultQueue()).to(f1Exchange()).with(RACE_RESULT_KEY);
    }
    @Bean public Binding scoringResultAmendedBinding() {
        return BindingBuilder.bind(scoringResultAmendedQueue()).to(f1Exchange()).with(RESULT_AMENDED_KEY);
    }
    @Bean public Binding scoringRaceResultDlqBinding() {
        return BindingBuilder.bind(scoringRaceResultDlq()).to(scoringDlx()).with(SCORING_RACE_RESULT_QUEUE);
    }
    @Bean public Binding scoringResultAmendedDlqBinding() {
        return BindingBuilder.bind(scoringResultAmendedDlq()).to(scoringDlx()).with(SCORING_RESULT_AMENDED_QUEUE);
    }

    // ── notification domain ──────────────────────────────────────────────────
    @Bean public Queue notificationSessionCompleteQueue() {
        return durableQueue(NOTIFICATION_SESSION_COMPLETE_QUEUE, NOTIFICATION_DLX);
    }
    @Bean public Queue notificationRaceResultQueue() {
        return durableQueue(NOTIFICATION_RACE_RESULT_QUEUE, NOTIFICATION_DLX);
    }
    @Bean public Queue notificationResultAmendedQueue() {
        return durableQueue(NOTIFICATION_RESULT_AMENDED_QUEUE, NOTIFICATION_DLX);
    }
    @Bean public Queue notificationSessionCompleteDlq() { return new Queue(NOTIFICATION_SESSION_COMPLETE_QUEUE + ".dlq", true); }
    @Bean public Queue notificationRaceResultDlq()      { return new Queue(NOTIFICATION_RACE_RESULT_QUEUE + ".dlq", true); }
    @Bean public Queue notificationResultAmendedDlq()   { return new Queue(NOTIFICATION_RESULT_AMENDED_QUEUE + ".dlq", true); }
    @Bean public Binding notificationSessionCompleteBinding() {
        return BindingBuilder.bind(notificationSessionCompleteQueue()).to(f1Exchange()).with(SESSION_COMPLETE_KEY);
    }
    @Bean public Binding notificationRaceResultBinding() {
        return BindingBuilder.bind(notificationRaceResultQueue()).to(f1Exchange()).with(RACE_RESULT_KEY);
    }
    @Bean public Binding notificationResultAmendedBinding() {
        return BindingBuilder.bind(notificationResultAmendedQueue()).to(f1Exchange()).with(RESULT_AMENDED_KEY);
    }
    @Bean public Binding notificationSessionCompleteDlqBinding() {
        return BindingBuilder.bind(notificationSessionCompleteDlq()).to(notificationDlx()).with(NOTIFICATION_SESSION_COMPLETE_QUEUE);
    }
    @Bean public Binding notificationRaceResultDlqBinding() {
        return BindingBuilder.bind(notificationRaceResultDlq()).to(notificationDlx()).with(NOTIFICATION_RACE_RESULT_QUEUE);
    }
    @Bean public Binding notificationResultAmendedDlqBinding() {
        return BindingBuilder.bind(notificationResultAmendedDlq()).to(notificationDlx()).with(NOTIFICATION_RESULT_AMENDED_QUEUE);
    }

    // ── analytics domain ─────────────────────────────────────────────────────
    @Bean public Queue analyticsSessionCompleteQueue() {
        return durableQueue(ANALYTICS_SESSION_COMPLETE_QUEUE, ANALYTICS_DLX);
    }
    @Bean public Queue analyticsRaceResultQueue() {
        return durableQueue(ANALYTICS_RACE_RESULT_QUEUE, ANALYTICS_DLX);
    }
    @Bean public Queue analyticsSessionCompleteDlq() { return new Queue(ANALYTICS_SESSION_COMPLETE_QUEUE + ".dlq", true); }
    @Bean public Queue analyticsRaceResultDlq()      { return new Queue(ANALYTICS_RACE_RESULT_QUEUE + ".dlq", true); }
    @Bean public Binding analyticsSessionCompleteBinding() {
        return BindingBuilder.bind(analyticsSessionCompleteQueue()).to(f1Exchange()).with(SESSION_COMPLETE_KEY);
    }
    @Bean public Binding analyticsRaceResultBinding() {
        return BindingBuilder.bind(analyticsRaceResultQueue()).to(f1Exchange()).with(RACE_RESULT_KEY);
    }
    @Bean public Binding analyticsSessionCompleteDlqBinding() {
        return BindingBuilder.bind(analyticsSessionCompleteDlq()).to(analyticsDlx()).with(ANALYTICS_SESSION_COMPLETE_QUEUE);
    }
    @Bean public Binding analyticsRaceResultDlqBinding() {
        return BindingBuilder.bind(analyticsRaceResultDlq()).to(analyticsDlx()).with(ANALYTICS_RACE_RESULT_QUEUE);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
