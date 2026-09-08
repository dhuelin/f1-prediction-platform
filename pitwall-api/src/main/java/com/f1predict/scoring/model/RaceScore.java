package com.f1predict.scoring.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "race_scores")
public class RaceScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID leagueId;

    @Column(nullable = false, length = 50)
    private String raceId;

    @Column(nullable = false, length = 20)
    private String sessionType;

    @Column(name = "top_n_points", nullable = false)
    private int topNPoints = 0;

    @Column(name = "bonus_points", nullable = false)
    private int bonusPoints = 0;

    @Column(name = "total_points", nullable = false)
    private int totalPoints = 0;

    @Column(name = "is_partial_distance", nullable = false)
    private boolean partialDistance = false;

    @Column(name = "is_cancelled", nullable = false)
    private boolean cancelled = false;

    @Column(nullable = false)
    private Instant scoredAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getLeagueId() { return leagueId; }
    public void setLeagueId(UUID leagueId) { this.leagueId = leagueId; }

    public String getRaceId() { return raceId; }
    public void setRaceId(String raceId) { this.raceId = raceId; }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    public int getTopNPoints() { return topNPoints; }
    public void setTopNPoints(int topNPoints) { this.topNPoints = topNPoints; }

    public int getBonusPoints() { return bonusPoints; }
    public void setBonusPoints(int bonusPoints) { this.bonusPoints = bonusPoints; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public boolean isPartialDistance() { return partialDistance; }
    public void setPartialDistance(boolean partialDistance) { this.partialDistance = partialDistance; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public Instant getScoredAt() { return scoredAt; }
    public void setScoredAt(Instant scoredAt) { this.scoredAt = scoredAt; }
}
