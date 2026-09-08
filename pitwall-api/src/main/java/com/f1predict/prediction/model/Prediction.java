package com.f1predict.prediction.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "predictions")
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String raceId;

    @Column(nullable = false, length = 20)
    private String sessionType;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(nullable = false)
    private Instant submittedAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "prediction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PredictionEntry> entries = new ArrayList<>();

    @OneToMany(mappedBy = "prediction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BonusBet> bonusBets = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getRaceId() {
        return raceId;
    }

    public void setRaceId(String raceId) {
        this.raceId = raceId;
    }

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<PredictionEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<PredictionEntry> entries) {
        this.entries = entries;
    }

    public List<BonusBet> getBonusBets() {
        return bonusBets;
    }

    public void setBonusBets(List<BonusBet> bonusBets) {
        this.bonusBets = bonusBets;
    }
}
