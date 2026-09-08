package com.f1predict.league.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "scoring_configs")
public class ScoringConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID leagueId;

    @Column(nullable = false)
    private int effectiveFromRace = 1;

    @Column(nullable = false)
    private int predictionDepth = 10;

    @Column(nullable = false)
    private int exactPositionPoints = 10;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String offsetTiers = "[{\"offset\":1,\"points\":7},{\"offset\":2,\"points\":2}]";

    @Column(nullable = false)
    private int inRangePoints = 1;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal betMultiplier = new BigDecimal("2.0");

    @Column(nullable = false, columnDefinition = "TEXT")
    private String activeBets = "{\"fastestLap\":true,\"dnfDsqDns\":true,\"scDeployed\":true,\"scCount\":true}";

    @Column(nullable = false)
    private boolean sprintScoringEnabled = true;

    private Integer maxStakePerBet;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getLeagueId() { return leagueId; }
    public void setLeagueId(UUID leagueId) { this.leagueId = leagueId; }

    public int getEffectiveFromRace() { return effectiveFromRace; }
    public void setEffectiveFromRace(int effectiveFromRace) { this.effectiveFromRace = effectiveFromRace; }

    public int getPredictionDepth() { return predictionDepth; }
    public void setPredictionDepth(int predictionDepth) { this.predictionDepth = predictionDepth; }

    public int getExactPositionPoints() { return exactPositionPoints; }
    public void setExactPositionPoints(int exactPositionPoints) { this.exactPositionPoints = exactPositionPoints; }

    public String getOffsetTiers() { return offsetTiers; }
    public void setOffsetTiers(String offsetTiers) { this.offsetTiers = offsetTiers; }

    public int getInRangePoints() { return inRangePoints; }
    public void setInRangePoints(int inRangePoints) { this.inRangePoints = inRangePoints; }

    public BigDecimal getBetMultiplier() { return betMultiplier; }
    public void setBetMultiplier(BigDecimal betMultiplier) { this.betMultiplier = betMultiplier; }

    public String getActiveBets() { return activeBets; }
    public void setActiveBets(String activeBets) { this.activeBets = activeBets; }

    public boolean isSprintScoringEnabled() { return sprintScoringEnabled; }
    public void setSprintScoringEnabled(boolean sprintScoringEnabled) { this.sprintScoringEnabled = sprintScoringEnabled; }

    public Integer getMaxStakePerBet() { return maxStakePerBet; }
    public void setMaxStakePerBet(Integer maxStakePerBet) { this.maxStakePerBet = maxStakePerBet; }
}
