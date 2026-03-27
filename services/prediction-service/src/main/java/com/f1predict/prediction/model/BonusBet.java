package com.f1predict.prediction.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bonus_bets")
public class BonusBet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prediction_id", nullable = false)
    private Prediction prediction;

    @Column(nullable = false, length = 30)
    private String betType;

    @Column(nullable = false)
    private int stake;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String betValue;

    @Column(nullable = false)
    private boolean settled = false;

    private Boolean won;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Prediction getPrediction() {
        return prediction;
    }

    public void setPrediction(Prediction prediction) {
        this.prediction = prediction;
    }

    public String getBetType() {
        return betType;
    }

    public void setBetType(String betType) {
        this.betType = betType;
    }

    public int getStake() {
        return stake;
    }

    public void setStake(int stake) {
        this.stake = stake;
    }

    public String getBetValue() {
        return betValue;
    }

    public void setBetValue(String betValue) {
        this.betValue = betValue;
    }

    public boolean isSettled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }

    public Boolean getWon() {
        return won;
    }

    public void setWon(Boolean won) {
        this.won = won;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
