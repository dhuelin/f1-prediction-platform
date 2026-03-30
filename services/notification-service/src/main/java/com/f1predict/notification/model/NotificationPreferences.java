package com.f1predict.notification.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreferences {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false) private boolean predictionReminder = true;
    @Column(nullable = false) private boolean raceStart = true;
    @Column(nullable = false) private boolean resultsPublished = true;
    @Column(nullable = false) private boolean scoreAmended = true;

    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();

    protected NotificationPreferences() {}

    public NotificationPreferences(UUID userId) {
        this.userId = userId;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public boolean isPredictionReminder() { return predictionReminder; }
    public boolean isRaceStart() { return raceStart; }
    public boolean isResultsPublished() { return resultsPublished; }
    public boolean isScoreAmended() { return scoreAmended; }

    public void update(boolean predictionReminder, boolean raceStart,
                       boolean resultsPublished, boolean scoreAmended) {
        this.predictionReminder = predictionReminder;
        this.raceStart = raceStart;
        this.resultsPublished = resultsPublished;
        this.scoreAmended = scoreAmended;
        this.updatedAt = Instant.now();
    }
}
