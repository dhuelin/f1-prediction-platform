package com.f1predict.scoring;

import com.f1predict.scoring.dto.*;
import com.f1predict.scoring.service.BonusBetScoreEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BonusBetScoreEngineTest {

    private final BonusBetScoreEngine engine = new BonusBetScoreEngine();

    private static final ScoringConfigData CONFIG = new ScoringConfigData(
        10, 10,
        List.of(new OffsetTier(1, 7), new OffsetTier(2, 2)),
        1, new BigDecimal("2.0"),
        true, true, true, true, true, null
    );

    private static RaceResultData result(String fastestLap, List<String> dnfs, int safetyCars) {
        return new RaceResultData(List.of(), fastestLap, dnfs, safetyCars, false, false);
    }

    @Test
    void fastestLap_correctDriver_wins() {
        BetData bet = new BetData("FASTEST_LAP", 50, "VER");
        int points = engine.scoreBets(List.of(bet), result("VER", List.of(), 0), CONFIG);
        assertThat(points).isEqualTo(100); // 50 * 2.0
    }

    @Test
    void fastestLap_wrongDriver_loses() {
        BetData bet = new BetData("FASTEST_LAP", 50, "HAM");
        int points = engine.scoreBets(List.of(bet), result("VER", List.of(), 0), CONFIG);
        assertThat(points).isEqualTo(-50);
    }

    @Test
    void dnfDsqDns_driverRetires_wins() {
        BetData bet = new BetData("DNF_DSQ_DNS", 30, "BOT");
        int points = engine.scoreBets(List.of(bet), result(null, List.of("BOT", "MAG"), 0), CONFIG);
        assertThat(points).isEqualTo(60); // 30 * 2.0
    }

    @Test
    void dnfDsqDns_driverFinishes_loses() {
        BetData bet = new BetData("DNF_DSQ_DNS", 30, "VER");
        int points = engine.scoreBets(List.of(bet), result(null, List.of("BOT"), 0), CONFIG);
        assertThat(points).isEqualTo(-30);
    }

    @Test
    void scDeployed_betTrue_scHappened_wins() {
        BetData bet = new BetData("SC_DEPLOYED", 20, "true");
        int points = engine.scoreBets(List.of(bet), result(null, List.of(), 1), CONFIG);
        assertThat(points).isEqualTo(40);
    }

    @Test
    void scDeployed_betTrue_noSC_loses() {
        BetData bet = new BetData("SC_DEPLOYED", 20, "true");
        int points = engine.scoreBets(List.of(bet), result(null, List.of(), 0), CONFIG);
        assertThat(points).isEqualTo(-20);
    }

    @Test
    void scDeployed_betFalse_noSC_wins() {
        BetData bet = new BetData("SC_DEPLOYED", 20, "false");
        int points = engine.scoreBets(List.of(bet), result(null, List.of(), 0), CONFIG);
        assertThat(points).isEqualTo(40);
    }

    @Test
    void scCount_exactMatch_wins() {
        BetData bet = new BetData("SC_COUNT", 25, "2");
        int points = engine.scoreBets(List.of(bet), result(null, List.of(), 2), CONFIG);
        assertThat(points).isEqualTo(50);
    }

    @Test
    void scCount_wrongCount_loses() {
        BetData bet = new BetData("SC_COUNT", 25, "3");
        int points = engine.scoreBets(List.of(bet), result(null, List.of(), 2), CONFIG);
        assertThat(points).isEqualTo(-25);
    }

    @Test
    void multipleBets_sumAllOutcomes() {
        List<BetData> bets = List.of(
            new BetData("FASTEST_LAP", 50, "VER"),   // win → +100
            new BetData("DNF_DSQ_DNS", 30, "HAM"),   // lose (HAM finishes) → -30
            new BetData("SC_DEPLOYED", 20, "true")    // win → +40
        );
        RaceResultData res = result("VER", List.of("BOT"), 1);
        int points = engine.scoreBets(bets, res, CONFIG);
        assertThat(points).isEqualTo(100 - 30 + 40); // 110
    }

    @Test
    void disabledBetType_skipped() {
        ScoringConfigData noFastestLap = new ScoringConfigData(
            10, 10, List.of(), 1, new BigDecimal("2.0"),
            false, true, true, true, true, null); // fastestLap disabled
        BetData bet = new BetData("FASTEST_LAP", 50, "VER");
        int points = engine.scoreBets(List.of(bet), result("VER", List.of(), 0), noFastestLap);
        assertThat(points).isEqualTo(0); // skipped entirely
    }

    @Test
    void emptyBets_returnsZero() {
        int points = engine.scoreBets(List.of(), result("VER", List.of(), 0), CONFIG);
        assertThat(points).isEqualTo(0);
    }

    @Test
    void multiplierFloorRounding() {
        // 3 * 2.0 = 6.0 → floor → 6 (exact, no rounding needed; verify floor still applied)
        ScoringConfigData oddMultiplier = new ScoringConfigData(
            10, 10, List.of(), 1, new BigDecimal("1.5"),
            true, true, true, true, true, null);
        BetData bet = new BetData("FASTEST_LAP", 3, "VER"); // 3 * 1.5 = 4.5 → floor → 4
        int points = engine.scoreBets(List.of(bet), result("VER", List.of(), 0), oddMultiplier);
        assertThat(points).isEqualTo(4);
    }
}
