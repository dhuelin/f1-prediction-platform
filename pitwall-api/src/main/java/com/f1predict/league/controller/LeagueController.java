package com.f1predict.league.controller;

import com.f1predict.league.dto.*;
import com.f1predict.league.service.LeagueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/leagues")
public class LeagueController {

    private final LeagueService leagueService;

    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    // #65 — Create league
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeagueResponse create(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateLeagueRequest request) {
        return leagueService.createLeague(userId, request);
    }

    // #65 — Browse public leagues
    @GetMapping
    public List<LeagueResponse> list() {
        return leagueService.listPublicLeagues();
    }

    // #65 — Get single league
    @GetMapping("/{leagueId}")
    public LeagueResponse get(
            @PathVariable UUID leagueId,
            @RequestHeader("X-User-Id") UUID userId) {
        return leagueService.getLeague(leagueId, userId);
    }

    // #66 — Join league
    @PostMapping("/{leagueId}/join")
    @ResponseStatus(HttpStatus.CREATED)
    public LeagueResponse join(
            @PathVariable UUID leagueId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody(required = false) JoinLeagueRequest request) {
        return leagueService.joinLeague(userId, leagueId, request);
    }

    // #67 — Get scoring config
    @GetMapping("/{leagueId}/config")
    public ScoringConfigResponse getConfig(@PathVariable UUID leagueId) {
        return leagueService.getConfig(leagueId);
    }

    // #67 — Update scoring config (admin only), next race number required
    @PutMapping("/{leagueId}/config")
    public ScoringConfigResponse updateConfig(
            @PathVariable UUID leagueId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam int nextRace,
            @Valid @RequestBody UpdateScoringConfigRequest request) {
        return leagueService.updateConfig(leagueId, userId, request, nextRace);
    }

    // #69 — Remove member (admin only)
    @DeleteMapping("/{leagueId}/members/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID leagueId,
            @PathVariable UUID targetUserId,
            @RequestHeader("X-User-Id") UUID userId) {
        leagueService.removeMember(leagueId, userId, targetUserId);
    }

    // #69 — Transfer admin
    @PostMapping("/{leagueId}/admin/transfer/{newAdminId}")
    public LeagueResponse transferAdmin(
            @PathVariable UUID leagueId,
            @PathVariable UUID newAdminId,
            @RequestHeader("X-User-Id") UUID userId) {
        return leagueService.transferAdmin(leagueId, userId, newAdminId);
    }
}
