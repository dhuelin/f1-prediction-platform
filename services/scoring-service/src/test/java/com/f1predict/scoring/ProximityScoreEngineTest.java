package com.f1predict.scoring;

import com.f1predict.scoring.dto.*;
import com.f1predict.scoring.service.ProximityScoreEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProximityScoreEngineTest {

    private final ProximityScoreEngine engine = new ProximityScoreEngine();

    private static final ScoringConfigData DEFAULT_CONFIG = new ScoringConfigData(
        10, 10,
        List.of(new OffsetTier(1, 7), new OffsetTier(2, 2)),
        1, new BigDecimal("2.0"),
        true, true, true, true, true, null
    );

    private static List<DriverResult> results(String... codes) {
        return List.of(codes).stream().map(DriverResult::new).toList();
    }

    @Test
    void exactMatch_awardsExactPoints() {
        // Predict VER 1st, VER actually 1st → 10 pts
        int score = engine.scoreTopN(List.of("VER"), results("VER", "HAM"), DEFAULT_CONFIG);
        assertThat(score).isEqualTo(10);
    }

    @Test
    void oneOff_awardsTier1Points() {
        // Predict HAM 1st (actually 2nd) → 1 off → 7 pts
        int score = engine.scoreTopN(List.of("HAM"), results("VER", "HAM"), DEFAULT_CONFIG);
        assertThat(score).isEqualTo(7);
    }

    @Test
    void twoOff_awardsTier2Points() {
        // Predict LEC 1st (actually 3rd) → 2 off → 2 pts
        int score = engine.scoreTopN(List.of("LEC"), results("VER", "HAM", "LEC"), DEFAULT_CONFIG);
        assertThat(score).isEqualTo(2);
    }

    @Test
    void inRange_outsideTiers_awardsInRangePoints() {
        // Predict VER 1st (actually 5th) → 4 off, outside tiers, within depth → 1 pt
        int score = engine.scoreTopN(
            List.of("VER"),
            results("HAM", "LEC", "NOR", "PIA", "VER"),
            DEFAULT_CONFIG);
        assertThat(score).isEqualTo(1);
    }

    @Test
    void driverOutsidePredictedRange_awardsZero() {
        // Predict top 3: VER, HAM, LEC. RIC finishes 2nd — not in my prediction → 0 pts for RIC slot
        // Predict VER 1st (correct), HAM 2nd (RIC is actually 2nd, HAM is elsewhere)
        // Prediction: VER, HAM, LEC. Actual: VER, RIC, NOR
        // VER: exact → 10. HAM: not in actual top-3 (actual pos is >3) → 0. LEC: not in actual top-3 → 0
        int score = engine.scoreTopN(
            List.of("VER", "HAM", "LEC"),
            results("VER", "RIC", "NOR"),
            DEFAULT_CONFIG);
        assertThat(score).isEqualTo(10); // only VER exact
    }

    @Test
    void dnfDriver_awardsZero() {
        // Predict HAM 1st; HAM DNFs (not in classified results)
        int score = engine.scoreTopN(
            List.of("HAM"),
            results("VER", "LEC"), // HAM not present
            DEFAULT_CONFIG);
        assertThat(score).isEqualTo(0);
    }

    @Test
    void multipleDrivers_sumAllPoints() {
        // Prediction: VER(1), HAM(2), LEC(3), NOR(4), PIA(5)
        // Actual:     VER(1), LEC(2), HAM(3), RUS(4), NOR(5)
        // VER: actual=1, predicted=1 → exact → 10
        // HAM: actual=3, predicted=2 → 1-off → 7
        // LEC: actual=2, predicted=3 → 1-off → 7
        // NOR: actual=5, predicted=4 → 1-off → 7
        // PIA: not in actual top-5 → 0
        int score = engine.scoreTopN(
            List.of("VER", "HAM", "LEC", "NOR", "PIA"),
            results("VER", "LEC", "HAM", "RUS", "NOR"),
            DEFAULT_CONFIG);
        assertThat(score).isEqualTo(10 + 7 + 7 + 7); // 31
    }

    @Test
    void partialDistanceHandledByOrchestrator_engineUnaffected() {
        // Engine doesn't halve — orchestrator does. Verify engine returns full points.
        int score = engine.scoreTopN(List.of("VER"), results("VER"), DEFAULT_CONFIG);
        assertThat(score).isEqualTo(10);
    }

    @Test
    void depthLimit_ignoresDriversBeyondDepth() {
        // Config depth=2; prediction has 5 drivers; only first 2 scored
        ScoringConfigData shallowConfig = new ScoringConfigData(
            2, 10,
            List.of(new OffsetTier(1, 7)),
            1, new BigDecimal("2.0"),
            true, true, true, true, true, null);
        // Predict: VER(1), HAM(2), LEC(3), NOR(4), PIA(5)
        // Only VER and HAM scored; LEC/NOR/PIA ignored
        int score = engine.scoreTopN(
            List.of("VER", "HAM", "LEC", "NOR", "PIA"),
            results("VER", "HAM", "LEC", "NOR", "PIA"),
            shallowConfig);
        assertThat(score).isEqualTo(20); // VER exact (10) + HAM exact (10)
    }
}
