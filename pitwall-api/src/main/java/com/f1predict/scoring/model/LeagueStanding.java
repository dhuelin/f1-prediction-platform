package com.f1predict.scoring.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "league_standings")
public class LeagueStanding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID leagueId;

    @Column(nullable = false)
    private int totalPoints = 0;

    @Column(nullable = false)
    private int rank = 0;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getLeagueId() { return leagueId; }
    public void setLeagueId(UUID leagueId) { this.leagueId = leagueId; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
