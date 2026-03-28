package com.f1predict.scoring.client;

import com.f1predict.scoring.dto.LeagueMemberData;
import com.f1predict.scoring.dto.ScoringConfigData;
import com.f1predict.scoring.dto.OffsetTier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class LeagueClient {

    private static final Logger log = LoggerFactory.getLogger(LeagueClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LeagueClient(@Value("${services.league-url}") String leagueUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder()
            .baseUrl(leagueUrl)
            .requestFactory(factory)
            .build();
    }

    public List<LeagueMemberData> getMembers(UUID leagueId) {
        try {
            List<LeagueMemberData> result = restClient.get()
                .uri("/leagues/{leagueId}/internal/members", leagueId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
            return result != null ? result : Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Failed to fetch members for league {} — skipping scoring", leagueId, e);
            return Collections.emptyList();
        }
    }

    public ScoringConfigData getConfig(UUID leagueId, int raceNumber) {
        try {
            JsonNode node = restClient.get()
                .uri("/leagues/{leagueId}/internal/config?raceNumber={raceNumber}", leagueId, raceNumber)
                .retrieve()
                .body(JsonNode.class);
            if (node == null) return defaultConfig();
            return parseConfig(node);
        } catch (RestClientException e) {
            log.error("Failed to fetch scoring config for league {} — using defaults", leagueId, e);
            return defaultConfig();
        }
    }

    private ScoringConfigData parseConfig(JsonNode node) {
        try {
            List<OffsetTier> tiers = objectMapper.convertValue(
                node.get("offsetTiers"),
                new TypeReference<>() {});
            return new ScoringConfigData(
                node.path("predictionDepth").asInt(10),
                node.path("exactPositionPoints").asInt(10),
                tiers,
                node.path("inRangePoints").asInt(1),
                new BigDecimal(node.path("betMultiplier").asText("2.0")),
                node.path("activeBets").path("fastestLap").asBoolean(true),
                node.path("activeBets").path("dnfDsqDns").asBoolean(true),
                node.path("activeBets").path("scDeployed").asBoolean(true),
                node.path("activeBets").path("scCount").asBoolean(true),
                node.path("sprintScoringEnabled").asBoolean(true),
                node.path("maxStakePerBet").isNull() ? null : node.path("maxStakePerBet").asInt()
            );
        } catch (Exception e) {
            log.warn("Failed to parse scoring config — using defaults", e);
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
