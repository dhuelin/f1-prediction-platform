package com.f1predict.league.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Published contract of the league domain for other domains in this app.
 *
 * Replaces two HTTP calls scoring-service used to make — to
 * GET /leagues/{id}/internal/members and GET /leagues/{id}/internal/config.
 * Neither endpoint was ever implemented, so both calls 404'd and were swallowed
 * by the client's fail-soft error handling: scoring silently saw zero members
 * and fell back to default scoring rules for every league. Implemented by
 * LeagueService.
 */
public interface LeagueDirectory {

    /** Every member of a league, with the catch-up points granted when they joined. */
    List<LeagueMemberSnapshot> getMembers(UUID leagueId);

    /**
     * The scoring config in force for a given race number — the newest config whose
     * effectiveFromRace is at or before raceNumber. Falls back to the league's
     * earliest config when a change is scheduled but not yet effective, and is
     * empty only when the league has no config rows at all.
     */
    Optional<LeagueScoringConfig> getEffectiveConfig(UUID leagueId, int raceNumber);
}
