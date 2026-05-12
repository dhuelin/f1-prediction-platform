package com.f1predict.analytics.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "race_analytics_events")
public class RaceAnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "race_id", nullable = false, length = 64)
    private String raceId;

    @Column(name = "league_id", length = 64)
    private String leagueId;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected RaceAnalyticsEvent() {}

    public RaceAnalyticsEvent(String eventType, String raceId, String leagueId, String payload) {
        this.eventType = eventType;
        this.raceId = raceId;
        this.leagueId = leagueId;
        this.payload = payload;
        this.occurredAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public String getRaceId() { return raceId; }
    public String getLeagueId() { return leagueId; }
    public String getPayload() { return payload; }
    public Instant getOccurredAt() { return occurredAt; }
}
