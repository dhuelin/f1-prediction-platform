package com.f1predict.scoring.internal;

import com.f1predict.prediction.api.PredictionDirectory;
import com.f1predict.scoring.dto.BetData;
import com.f1predict.scoring.dto.PredictionData;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reads locked predictions for scoring.
 *
 * Was an HTTP call to prediction-service; now a direct call into the prediction
 * domain, mapping its response onto the scoring domain's own DTOs. The mapping
 * lives here so the scoring engines keep working against PredictionData.
 */
@Component
public class PredictionLookup {

    private final PredictionDirectory predictions;

    public PredictionLookup(PredictionDirectory predictions) {
        this.predictions = predictions;
    }

    public List<PredictionData> getPredictions(String raceId, String sessionType) {
        return predictions.getLockedPredictions(raceId, sessionType).stream()
            .map(p -> new PredictionData(
                p.userId(),
                p.sessionType(),
                p.rankedDriverCodes(),
                p.bets().stream()
                    .map(b -> new BetData(b.betType(), b.stake(), b.betValue()))
                    .toList(),
                p.updatedAt()))
            .toList();
    }
}
