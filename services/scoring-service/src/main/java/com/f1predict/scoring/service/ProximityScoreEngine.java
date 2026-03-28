package com.f1predict.scoring.service;

import com.f1predict.scoring.dto.OffsetTier;
import com.f1predict.scoring.dto.ScoringConfigData;
import com.f1predict.scoring.dto.DriverResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure scoring engine for top-N proximity predictions.
 *
 * For each driver in the user's predicted list (1-based position):
 *   - Exact position match            → exactPositionPoints
 *   - 1 off (|predicted - actual| = 1) → tier1 points (default 7)
 *   - 2 off (|predicted - actual| = 2) → tier2 points (default 2)
 *   - In predicted range, outside tiers → inRangePoints (default 1)
 *   - Driver finished outside top-N     → 0 points
 *   - Driver DNF / not in actual results → 0 points
 */
@Component
public class ProximityScoreEngine {

    /**
     * @param predictedDriverCodes ordered list of predicted driver codes (position 1 = index 0)
     * @param actual               list of DriverResult in finishing order (position 1 = index 0), DNFs excluded
     * @param config               scoring config for this league / race
     * @return total top-N points for this prediction
     */
    public int scoreTopN(List<String> predictedDriverCodes, List<DriverResult> actual, ScoringConfigData config) {
        int depth = config.predictionDepth();

        // Build map: driverCode -> 1-based actual finish position (only classified finishers)
        Map<String, Integer> actualPositions = new HashMap<>();
        for (int i = 0; i < actual.size(); i++) {
            actualPositions.put(actual.get(i).driverCode(), i + 1);
        }

        int total = 0;
        int limit = Math.min(predictedDriverCodes.size(), depth);
        for (int i = 0; i < limit; i++) {
            String driver = predictedDriverCodes.get(i);
            int predictedPos = i + 1;

            Integer actualPos = actualPositions.get(driver);
            if (actualPos == null) {
                // Driver DNF'd or not in classified results — 0 points
                continue;
            }

            if (actualPos.equals(predictedPos)) {
                total += config.exactPositionPoints();
            } else {
                int offset = Math.abs(actualPos - predictedPos);
                Integer tierPoints = getTierPoints(offset, config.offsetTiers());
                if (tierPoints != null) {
                    total += tierPoints;
                } else if (actualPos <= depth) {
                    // Driver finished within the predicted range but outside proximity tiers
                    total += config.inRangePoints();
                }
                // else: driver finished outside top-N range → 0 points
            }
        }
        return total;
    }

    private Integer getTierPoints(int offset, List<OffsetTier> tiers) {
        // Find the tier with the smallest offset threshold that still covers this offset.
        // "Tightest match" means the most specific tier, not the one with the highest points.
        // This matters for non-monotonic configs (e.g. [{offset:1,pts:5},{offset:2,pts:8}]):
        // a 1-off result should award 5 (tightest covering tier), not 8.
        if (tiers == null) return null;
        OffsetTier best = null;
        for (OffsetTier tier : tiers) {
            if (offset <= tier.offset()) {
                if (best == null || tier.offset() < best.offset()) {
                    best = tier;
                }
            }
        }
        return best != null ? best.points() : null;
    }
}
