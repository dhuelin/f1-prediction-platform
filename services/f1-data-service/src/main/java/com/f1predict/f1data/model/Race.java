package com.f1predict.f1data.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "races")
public class Race {

    @Id
    private String id;  // e.g. "2025-01" (season-round)

    @Column(nullable = false)
    private int season;

    @Column(nullable = false)
    private int round;

    @Column(nullable = false, length = 100)
    private String raceName;

    @Column(nullable = false, length = 100)
    private String circuitName;

    @Column(nullable = false, length = 100)
    private String country;

    private Instant raceDate;

    @Column(name = "is_sprint_weekend", nullable = false)
    private boolean sprintWeekend = false;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public String getRaceName() {
        return raceName;
    }

    public void setRaceName(String raceName) {
        this.raceName = raceName;
    }

    public String getCircuitName() {
        return circuitName;
    }

    public void setCircuitName(String circuitName) {
        this.circuitName = circuitName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Instant getRaceDate() {
        return raceDate;
    }

    public void setRaceDate(Instant raceDate) {
        this.raceDate = raceDate;
    }

    public boolean isSprintWeekend() {
        return sprintWeekend;
    }

    public void setSprintWeekend(boolean sprintWeekend) {
        this.sprintWeekend = sprintWeekend;
    }
}
