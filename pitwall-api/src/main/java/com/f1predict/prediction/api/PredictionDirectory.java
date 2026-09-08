package com.f1predict.prediction.api;

import com.f1predict.prediction.dto.InternalPredictionResponse;

import java.util.List;

/**
 * Published contract of the prediction domain for other domains in this app.
 *
 * Replaces the HTTP call scoring-service used to make to
 * GET /predictions/{raceId}/internal/all. Implemented by PredictionService.
 */
public interface PredictionDirectory {

    /** All locked predictions for a race and session type ("RACE" or "SPRINT"). */
    List<InternalPredictionResponse> getLockedPredictions(String raceId, String sessionType);
}
