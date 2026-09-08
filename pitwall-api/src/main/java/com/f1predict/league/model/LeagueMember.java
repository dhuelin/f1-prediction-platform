package com.f1predict.league.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "league_members")
public class LeagueMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID leagueId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Instant joinedAt = Instant.now();

    @Column(nullable = false)
    private int catchUpPoints = 0;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getLeagueId() { return leagueId; }
    public void setLeagueId(UUID leagueId) { this.leagueId = leagueId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }

    public int getCatchUpPoints() { return catchUpPoints; }
    public void setCatchUpPoints(int catchUpPoints) { this.catchUpPoints = catchUpPoints; }
}
