package com.f1predict.league;

import com.f1predict.auth.api.AuthDirectory;
import com.f1predict.auth.api.UserSummary;
import com.f1predict.league.dto.LeagueMemberResponse;
import com.f1predict.league.model.League;
import com.f1predict.league.model.LeagueMember;
import com.f1predict.league.repository.LeagueMemberRepository;
import com.f1predict.league.repository.LeagueRepository;
import com.f1predict.league.repository.ScoringConfigRepository;
import com.f1predict.league.service.LeagueService;
import com.f1predict.scoring.api.ScoringDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Covers GET /leagues/{leagueId}/members (#163), including its visibility rules. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeagueMembersTest {

    @Mock LeagueRepository leagueRepository;
    @Mock LeagueMemberRepository memberRepository;
    @Mock ScoringConfigRepository configRepository;
    @Mock ScoringDirectory scoring;
    @Mock AuthDirectory auth;

    LeagueService service;

    private final UUID leagueId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();
    private final UUID outsiderId = UUID.randomUUID();
    private final Instant joined = Instant.parse("2026-03-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        service = new LeagueService(leagueRepository, memberRepository, configRepository, scoring, auth);
    }

    private League league(String visibility) {
        League l = new League();
        l.setId(leagueId);
        l.setName("Test League");
        l.setVisibility(visibility);
        l.setAdminUserId(adminId);
        return l;
    }

    private LeagueMember member(UUID userId) {
        LeagueMember m = new LeagueMember();
        m.setLeagueId(leagueId);
        m.setUserId(userId);
        m.setJoinedAt(joined);
        return m;
    }

    private void stubRoster(String visibility) {
        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league(visibility)));
        when(memberRepository.findByLeagueId(leagueId))
            .thenReturn(List.of(member(adminId), member(memberId)));
        when(auth.getUsers(any())).thenReturn(List.of(
            new UserSummary(adminId, "denis"),
            new UserSummary(memberId, "alex")));
    }

    @Test
    void returnsRosterWithUsernamesAndAdminFlag() {
        stubRoster("PUBLIC");

        List<LeagueMemberResponse> members = service.getMembers(leagueId, memberId);

        assertThat(members).hasSize(2);
        assertThat(members).extracting(LeagueMemberResponse::username)
            .containsExactly("denis", "alex");
        assertThat(members).extracting(LeagueMemberResponse::isAdmin)
            .containsExactly(true, false);
        assertThat(members).allSatisfy(m -> assertThat(m.joinedAt()).isEqualTo(joined));
    }

    @Test
    void publicLeagueRosterIsVisibleToNonMembers() {
        stubRoster("PUBLIC");

        assertThat(service.getMembers(leagueId, outsiderId)).hasSize(2);
    }

    @Test
    void privateLeagueRosterIsVisibleToItsMembers() {
        stubRoster("PRIVATE");

        assertThat(service.getMembers(leagueId, memberId)).hasSize(2);
    }

    @Test
    void privateLeagueRosterIsHiddenFromNonMembers() {
        stubRoster("PRIVATE");

        assertThatThrownBy(() -> service.getMembers(leagueId, outsiderId))
            .isInstanceOf(SecurityException.class);
    }

    @Test
    void memberWithNoUserRowStillAppears() {
        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league("PUBLIC")));
        when(memberRepository.findByLeagueId(leagueId))
            .thenReturn(List.of(member(adminId), member(memberId)));
        // memberId's user row is gone; only the admin resolves
        when(auth.getUsers(any())).thenReturn(List.of(new UserSummary(adminId, "denis")));

        List<LeagueMemberResponse> members = service.getMembers(leagueId, adminId);

        assertThat(members).extracting(LeagueMemberResponse::username)
            .containsExactly("denis", "(unknown)");
    }

    @Test
    void unknownLeagueIsNotFound() {
        when(leagueRepository.findById(leagueId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMembers(leagueId, memberId))
            .isInstanceOf(NoSuchElementException.class);
    }
}
