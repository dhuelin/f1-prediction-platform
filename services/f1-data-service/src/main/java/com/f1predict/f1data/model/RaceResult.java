package com.f1predict.f1data.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "race_results")
public class RaceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Session.SessionType sessionType;

    @Column(nullable = false, length = 3)
    private String driverCode;

    private Integer finishPosition;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DriverStatus status;

    @Column(name = "is_fastest_lap", nullable = false)
    private boolean fastestLap = false;

    @Column(name = "is_partial_distance", nullable = false)
    private boolean partialDistance = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant amendedAt;

    public enum DriverStatus { CLASSIFIED, DNF, DSQ, DNS }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Race getRace() {
        return race;
    }

    public void setRace(Race race) {
        this.race = race;
    }

    public Session.SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(Session.SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public String getDriverCode() {
        return driverCode;
    }

    public void setDriverCode(String driverCode) {
        this.driverCode = driverCode;
    }

    public Integer getFinishPosition() {
        return finishPosition;
    }

    public void setFinishPosition(Integer finishPosition) {
        this.finishPosition = finishPosition;
    }

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }

    public boolean isFastestLap() {
        return fastestLap;
    }

    public void setFastestLap(boolean fastestLap) {
        this.fastestLap = fastestLap;
    }

    public boolean isPartialDistance() {
        return partialDistance;
    }

    public void setPartialDistance(boolean partialDistance) {
        this.partialDistance = partialDistance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAmendedAt() {
        return amendedAt;
    }

    public void setAmendedAt(Instant amendedAt) {
        this.amendedAt = amendedAt;
    }
}
