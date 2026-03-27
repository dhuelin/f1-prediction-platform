package com.f1predict.common.events;

// Published by Prediction Service when qualifying begins and all predictions are locked
public record PredictionLockedEvent(
    String raceId,
    SessionCompleteEvent.SessionType sessionType,
    int lockedCount  // number of predictions locked
) {}
