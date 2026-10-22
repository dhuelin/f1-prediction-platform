package com.f1predict.league.service;

import com.f1predict.auth.api.AuthDirectory;
import com.f1predict.auth.api.UserSummary;
import com.f1predict.league.api.LeagueDirectory;
import com.f1predict.league.api.LeagueMemberSnapshot;
import com.f1predict.league.api.LeagueScoringConfig;
import com.f1predict.scoring.api.ScoringDirectory;
import com.f1predict.league.dto.*;
import com.f1predict.league.model.League;
import com.f1predict.league.model.LeagueMember;
import com.f1predict.league.model.ScoringConfig;
import com.f1predict.league.repository.LeagueMemberRepository;
import com.f1predict.league.repository.LeagueRepository;
import com.f1predict.league.repository.ScoringConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class LeagueService implements LeagueDirectory {

    private final LeagueRepository leagueRepository;
    private final LeagueMemberRepository memberRepository;
    private final ScoringConfigRepository configRepository;
    private final ScoringDirectory scoring;
    private final AuthDirectory auth;

    public LeagueService(LeagueRepository leagueRepository,
                         LeagueMemberRepository memberRepository,
                         ScoringConfigRepository configRepository,
                         ScoringDirectory scoring,
                         AuthDirectory auth) {
        this.leagueRepository = leagueRepository;
        this.memberRepository = memberRepository;
        this.configRepository = configRepository;
        this.scoring = scoring;
        this.auth = auth;
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

        int catchUpPoints = scoring.getLeagueAveragePoints(leagueId);

        LeagueMember member = new LeagueMember();
        member.setLeagueId(leagueId);
        member.setUserId(userId);
        member.setCatchUpPoints(catchUpPoints);
        memberRepository.save(member);

        long count = memberRepository.countByLeagueId(leagueId);
        return toResponse(league, userId, count);
    }

    /**
     * Join whichever league owns an invite code.
     *
     * Both clients only ever have a code — the user is handed "PUNDITS42", not a
     * UUID — but the only join endpoint required the league id. The web app
     * passed the code as the id and the mobile app POSTed to a path that did not
     * exist, so joining a private league by code worked in neither.
     */
    @Transactional
    public LeagueResponse joinByInviteCode(UUID userId, String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new IllegalArgumentException("Invite code is required");
        }
        // Generated codes are upper-case; accept any casing the user types.
        String code = inviteCode.trim().toUpperCase();
        League league = leagueRepository.findByInviteCode(code)
            .orElseThrow(() -> new NoSuchElementException("No league found for that invite code"));
        return joinLeague(userId, league.getId(), new JoinLeagueRequest(code));
    }

    public List<LeagueResponse> listPublicLeagues() {
        return leagueRepository.findByVisibility("PUBLIC").stream()
            .map(l -> toResponse(l, null, memberRepository.countByLeagueId(l.getId())))
            .toList();
    }

    /**
     * Leagues the caller belongs to.
     *
     * The web app has always had a "My Leagues" tab, but the only list endpoint
     * returns PUBLIC leagues, so it showed the browse list in both tabs — a user's
     * own private leagues never appeared anywhere.
     */
    public List<LeagueResponse> listMyLeagues(UUID userId) {
        List<UUID> leagueIds = memberRepository.findByUserId(userId).stream()
            .map(LeagueMember::getLeagueId)
            .toList();
        if (leagueIds.isEmpty()) return List.of();
        return leagueRepository.findAllById(leagueIds).stream()
            .map(l -> toResponse(l, userId, memberRepository.countByLeagueId(l.getId())))
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

    /**
     * Alphabet for invite codes: upper-case letters and digits with the
     * easily-confused glyphs removed (no I, O, 0 or 1), since these get read aloud
     * and retyped. 32 symbols, so each character carries exactly 5 bits.
     */
    private static final char[] INVITE_CODE_ALPHABET =
        "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    /** 10 chars x 5 bits = 50 bits, and exactly the width of the invite_code column. */
    private static final int INVITE_CODE_LENGTH = 10;

    private static final SecureRandom INVITE_CODE_RANDOM = new SecureRandom();

    /**
     * An invite code no other league holds.
     *
     * The previous version took the first 8 hex characters of a UUID — 32 bits, and
     * only 16 symbols per character. POST /leagues/join resolves a league from its
     * code alone, so the code is the whole secret guarding a private league, and
     * /leagues/** has no rate limiter. 50 bits puts guessing out of reach.
     *
     * The retry exists so a collision cannot silently hand two leagues the same
     * code, which would make a join land in whichever one the query returned first.
     * At 50 bits it should never iterate; the UNIQUE constraint added in V12 is the
     * backstop if it somehow does.
     */
    private String generateInviteCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = randomInviteCode();
            if (leagueRepository.findByInviteCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate an unused invite code");
    }

    private static String randomInviteCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_CODE_ALPHABET[INVITE_CODE_RANDOM.nextInt(INVITE_CODE_ALPHABET.length)]);
        }
        return sb.toString();
    }

    /**
     * Members of a league, with usernames resolved from the auth domain (#163).
     *
     * A PRIVATE league's roster is visible only to its members; a PUBLIC league's
     * is visible to anyone signed in, matching how listPublicLeagues already
     * exposes public leagues.
     */
    public List<LeagueMemberResponse> getMembers(UUID leagueId, UUID requesterId) {
        League league = leagueRepository.findById(leagueId)
            .orElseThrow(() -> new NoSuchElementException("League not found"));

        List<LeagueMember> members = memberRepository.findByLeagueId(leagueId);

        if (!"PUBLIC".equals(league.getVisibility())) {
            boolean requesterIsMember = members.stream()
                .anyMatch(m -> m.getUserId().equals(requesterId));
            if (!requesterIsMember) {
                throw new SecurityException("Only members can view a private league's roster");
            }
        }

        Map<UUID, String> usernames = auth
            .getUsers(members.stream().map(LeagueMember::getUserId).toList())
            .stream()
            .collect(Collectors.toMap(UserSummary::userId, UserSummary::username));

        return members.stream()
            .map(m -> new LeagueMemberResponse(
                m.getUserId(),
                // A member whose user row has gone is still a member of the league;
                // render them rather than dropping them from the roster silently.
                usernames.getOrDefault(m.getUserId(), "(unknown)"),
                m.getUserId().equals(league.getAdminUserId()),
                m.getJoinedAt()))
            .toList();
    }

    // ── LeagueDirectory: in-app contract for the scoring domain ──────────────
    // These replace GET /leagues/{id}/internal/members and .../internal/config,
    // which scoring-service called over HTTP but which were never implemented.

    @Override
    public List<LeagueMemberSnapshot> getMembers(UUID leagueId) {
        return memberRepository.findByLeagueId(leagueId).stream()
            .map(m -> new LeagueMemberSnapshot(m.getUserId(), m.getCatchUpPoints()))
            .toList();
    }

    @Override
    public Optional<LeagueScoringConfig> getEffectiveConfig(UUID leagueId, int raceNumber) {
        return configRepository
            .findTopByLeagueIdAndEffectiveFromRaceLessThanEqualOrderByEffectiveFromRaceDesc(leagueId, raceNumber)
            .or(() -> configRepository.findTopByLeagueIdOrderByEffectiveFromRaceAsc(leagueId))
            .map(c -> new LeagueScoringConfig(
                c.getPredictionDepth(),
                c.getExactPositionPoints(),
                c.getOffsetTiers(),
                c.getInRangePoints(),
                c.getBetMultiplier(),
                c.getActiveBets(),
                c.isSprintScoringEnabled(),
                c.getMaxStakePerBet()));
    }
}
