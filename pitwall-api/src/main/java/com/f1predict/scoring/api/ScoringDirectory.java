package com.f1predict.scoring.api;

import java.util.UUID;

/**
 * Published contract of the scoring domain for other domains in this app.
 *
 * Replaces the HTTP calls prediction-service and league-service used to make to
 * GET /scores/balance/{userId}/leagues/{leagueId} and
 * GET /scores/leagues/{leagueId}/average-points. Both endpoints still exist on
 * ScoringController — the web app calls them — but in-app callers no longer go
 * through HTTP to reach them.
 */
public interface ScoringDirectory {

    /** A user's total points balance in a league, used to validate bet stakes. */
    int getBalance(UUID userId, UUID leagueId);

    /** Average total points across a league's members, for mid-season catch-up. */
    int getLeagueAveragePoints(UUID leagueId);
}
