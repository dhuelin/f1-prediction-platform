package com.f1predict.analytics.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "prediction_participation_stats",
       uniqueConstraints = @UniqueConstraint(columnNames = "race_id"))
public class PredictionParticipationStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "race_id", nullable = false, unique = true, length = 64)
    private String raceId;

    @Column(name = "locked_count", nullable = false)
    private int lockedCount;

    @Column(name = "locked_at", nullable = false)
    private Instant lockedAt;

    @Column(name = "session_type", length = 32)
    private String sessionType;

    protected PredictionParticipationStat() {}

    public PredictionParticipationStat(String raceId, int lockedCount, String sessionType) {
        this.raceId = raceId;
        this.lockedCount = lockedCount;
        this.sessionType = sessionType;
        this.lockedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getRaceId() { return raceId; }
    public int getLockedCount() { return lockedCount; }
    public Instant getLockedAt() { return lockedAt; }
    public String getSessionType() { return sessionType; }
}
