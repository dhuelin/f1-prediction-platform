package com.f1predict.scoring.internal;

import com.f1predict.league.api.LeagueDirectory;
import com.f1predict.league.api.LeagueScoringConfig;
import com.f1predict.scoring.dto.LeagueMemberData;
import com.f1predict.scoring.dto.OffsetTier;
import com.f1predict.scoring.dto.ScoringConfigData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Reads league membership and scoring rules for scoring.
 *
 * Was an HTTP call to two league-service endpoints that did not exist, so every
 * league silently scored with zero members and default rules. Now a direct call
 * into the league domain; the JSON parsing of offsetTiers / activeBets stays
 * here, where it was before.
 */
@Component
public class LeagueLookup {

    private static final Logger log = LoggerFactory.getLogger(LeagueLookup.class);

    private final LeagueDirectory leagues;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LeagueLookup(LeagueDirectory leagues) {
        this.leagues = leagues;
    }

    public List<LeagueMemberData> getMembers(UUID leagueId) {
        return leagues.getMembers(leagueId).stream()
            .map(m -> new LeagueMemberData(m.userId(), m.catchUpPoints()))
            .toList();
    }

    public ScoringConfigData getConfig(UUID leagueId, int raceNumber) {
        return leagues.getEffectiveConfig(leagueId, raceNumber)
            .map(this::toScoringConfig)
            .orElseGet(() -> {
                log.warn("League {} has no scoring config — using defaults", leagueId);
                return defaultConfig();
            });
    }

    private ScoringConfigData toScoringConfig(LeagueScoringConfig c) {
        try {
            List<OffsetTier> tiers = objectMapper.readValue(
                c.offsetTiersJson(), new TypeReference<>() {});
            JsonNode activeBets = objectMapper.readTree(c.activeBetsJson());
            return new ScoringConfigData(
                c.predictionDepth(),
                c.exactPositionPoints(),
                tiers,
                c.inRangePoints(),
                c.betMultiplier(),
                activeBets.path("fastestLap").asBoolean(true),
                activeBets.path("dnfDsqDns").asBoolean(true),
                activeBets.path("scDeployed").asBoolean(true),
                activeBets.path("scCount").asBoolean(true),
                c.sprintScoringEnabled(),
                c.maxStakePerBet()
            );
        } catch (Exception e) {
            log.warn("Failed to parse scoring config for league — using defaults", e);
            return defaultConfig();
        }
    }

    private ScoringConfigData defaultConfig() {
        return new ScoringConfigData(
            10, 10,
            List.of(new OffsetTier(1, 7), new OffsetTier(2, 2)),
            1, new BigDecimal("2.0"),
            true, true, true, true, true, null
        );
    }
}
