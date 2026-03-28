package com.f1predict.league.service;

import com.f1predict.league.client.ScoringClient;
import com.f1predict.league.dto.*;
import com.f1predict.league.model.League;
import com.f1predict.league.model.LeagueMember;
import com.f1predict.league.model.ScoringConfig;
import com.f1predict.league.repository.LeagueMemberRepository;
import com.f1predict.league.repository.LeagueRepository;
import com.f1predict.league.repository.ScoringConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class LeagueService {

    private final LeagueRepository leagueRepository;
    private final LeagueMemberRepository memberRepository;
    private final ScoringConfigRepository configRepository;
    private final ScoringClient scoringClient;

    public LeagueService(LeagueRepository leagueRepository,
                         LeagueMemberRepository memberRepository,
                         ScoringConfigRepository configRepository,
                         ScoringClient scoringClient) {
        this.leagueRepository = leagueRepository;
        this.memberRepository = memberRepository;
        this.configRepository = configRepository;
        this.scoringClient = scoringClient;
    }

    @Transactional
    public LeagueResponse createLeague(UUID adminUserId, CreateLeagueRequest req) {
        if (leagueRepository.findByName(req.name()).isPresent()) {
            throw new IllegalStateException("League name already taken");
        }
        String visibility = req.visibility() != null ? req.visibility().toUpperCase() : "PUBLIC";
        if (!visibility.equals("PUBLIC") && !visibility.equals("PRIVATE")) {
            throw new IllegalArgumentException("visibility must be PUBLIC or PRIVATE");
        }

        League league = new League();
        league.setName(req.name());
        league.setVisibility(visibility);
        league.setAdminUserId(adminUserId);

        if ("PRIVATE".equals(visibility)) {
            league.setInviteCode(generateInviteCode());
        }

        League saved = leagueRepository.save(league);

        // Creator is first member
        LeagueMember member = new LeagueMember();
        member.setLeagueId(saved.getId());
        member.setUserId(adminUserId);
        memberRepository.save(member);

        // Default scoring config
        ScoringConfig config = new ScoringConfig();
        config.setLeagueId(saved.getId());
        configRepository.save(config);

        return toResponse(saved, adminUserId, 1L);
    }

    @Transactional
    public LeagueResponse joinLeague(UUID userId, UUID leagueId, JoinLeagueRequest req) {
        League league = leagueRepository.findById(leagueId)
            .orElseThrow(() -> new NoSuchElementException("League not found"));

        if (memberRepository.findByLeagueIdAndUserId(leagueId, userId).isPresent()) {
            throw new IllegalStateException("Already a member of this league");
        }

        if ("PRIVATE".equals(league.getVisibility())) {
            if (req == null || req.inviteCode() == null || !req.inviteCode().equals(league.getInviteCode())) {
                throw new IllegalArgumentException("Invalid invite code");
            }
        }

        int catchUpPoints = scoringClient.getLeagueAveragePoints(leagueId);

        LeagueMember member = new LeagueMember();
        member.setLeagueId(leagueId);
        member.setUserId(userId);
        member.setCatchUpPoints(catchUpPoints);
        memberRepository.save(member);

        long count = memberRepository.countByLeagueId(leagueId);
        return toResponse(league, userId, count);
    }

    public List<LeagueResponse> listPublicLeagues() {
        return leagueRepository.findByVisibility("PUBLIC").stream()
            .map(l -> toResponse(l, null, memberRepository.countByLeagueId(l.getId())))
            .toList();
    }

    public LeagueResponse getLeague(UUID leagueId, UUID requesterId) {
        League league = leagueRepository.findById(leagueId)
            .orElseThrow(() -> new NoSuchElementException("League not found"));
        long count = memberRepository.countByLeagueId(leagueId);
        return toResponse(league, requesterId, count);
    }

    public ScoringConfigResponse getConfig(UUID leagueId) {
        leagueRepository.findById(leagueId)
            .orElseThrow(() -> new NoSuchElementException("League not found"));
        ScoringConfig config = configRepository
            .findTopByLeagueIdOrderByEffectiveFromRaceDesc(leagueId)
            .orElseThrow(() -> new NoSuchElementException("No scoring config found"));
        return toConfigResponse(config);
    }

    @Transactional
    public ScoringConfigResponse updateConfig(UUID leagueId, UUID adminUserId, UpdateScoringConfigRequest req, int nextRaceNumber) {
        League league = leagueRepository.findById(leagueId)
            .orElseThrow(() -> new NoSuchElementException("League not found"));
        if (!league.getAdminUserId().equals(adminUserId)) {
            throw new SecurityException("Only the league admin can update scoring config");
        }

        ScoringConfig config = new ScoringConfig();
        config.setLeagueId(leagueId);
        config.setEffectiveFromRace(nextRaceNumber);

        // Copy current config as baseline
        ScoringConfig current = configRepository
            .findTopByLeagueIdOrderByEffectiveFromRaceDesc(leagueId)
            .orElse(new ScoringConfig());

        config.setPredictionDepth(req.predictionDepth() != null ? req.predictionDepth() : current.getPredictionDepth());
        config.setExactPositionPoints(req.exactPositionPoints() != null ? req.exactPositionPoints() : current.getExactPositionPoints());
        config.setOffsetTiers(req.offsetTiers() != null ? req.offsetTiers() : current.getOffsetTiers());
        config.setInRangePoints(req.inRangePoints() != null ? req.inRangePoints() : current.getInRangePoints());
        config.setBetMultiplier(req.betMultiplier() != null ? req.betMultiplier() : current.getBetMultiplier());
        config.setActiveBets(req.activeBets() != null ? req.activeBets() : current.getActiveBets());
        config.setSprintScoringEnabled(req.sprintScoringEnabled() != null ? req.sprintScoringEnabled() : current.isSprintScoringEnabled());
        config.setMaxStakePerBet(req.maxStakePerBet());

        ScoringConfig saved = configRepository.save(config);
        return toConfigResponse(saved);
    }

    @Transactional
    public void removeMember(UUID leagueId, UUID adminUserId, UUID targetUserId) {
        League league = leagueRepository.findById(leagueId)
            .orElseThrow(() -> new NoSuchElementException("League not found"));
        if (!league.getAdminUserId().equals(adminUserId)) {
            throw new SecurityException("Only the league admin can remove members");
        }
        if (targetUserId.equals(adminUserId)) {
            throw new IllegalArgumentException("Admin cannot remove themselves — transfer admin first");
        }
        LeagueMember member = memberRepository.findByLeagueIdAndUserId(leagueId, targetUserId)
            .orElseThrow(() -> new NoSuchElementException("Member not found"));
        memberRepository.delete(member);
    }

    @Transactional
    public LeagueResponse transferAdmin(UUID leagueId, UUID currentAdminId, UUID newAdminId) {
        League league = leagueRepository.findById(leagueId)
            .orElseThrow(() -> new NoSuchElementException("League not found"));
        if (!league.getAdminUserId().equals(currentAdminId)) {
            throw new SecurityException("Only the current admin can transfer admin rights");
        }
        memberRepository.findByLeagueIdAndUserId(leagueId, newAdminId)
            .orElseThrow(() -> new NoSuchElementException("Target user is not a member of this league"));
        league.setAdminUserId(newAdminId);
        leagueRepository.save(league);
        long count = memberRepository.countByLeagueId(leagueId);
        return toResponse(league, currentAdminId, count);
    }

    private LeagueResponse toResponse(League league, UUID requesterId, long memberCount) {
        // Only expose invite code to the admin
        String inviteCode = (requesterId != null && requesterId.equals(league.getAdminUserId()))
            ? league.getInviteCode()
            : null;
        return new LeagueResponse(
            league.getId(),
            league.getName(),
            league.getVisibility(),
            inviteCode,
            league.getAdminUserId(),
            memberCount
        );
    }

    private ScoringConfigResponse toConfigResponse(ScoringConfig c) {
        return new ScoringConfigResponse(
            c.getId(),
            c.getEffectiveFromRace(),
            c.getPredictionDepth(),
            c.getExactPositionPoints(),
            c.getOffsetTiers(),
            c.getInRangePoints(),
            c.getBetMultiplier(),
            c.getActiveBets(),
            c.isSprintScoringEnabled(),
            c.getMaxStakePerBet()
        );
    }

    private String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
