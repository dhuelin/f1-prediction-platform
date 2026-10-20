package com.f1predict.league;

import com.f1predict.auth.api.AuthDirectory;
import com.f1predict.league.dto.LeagueResponse;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Covers GET /leagues/mine.
 *
 * The only list endpoint returned PUBLIC leagues, so the web app's "My Leagues"
 * tab showed the browse list and a user's own private leagues appeared nowhere.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MyLeaguesTest {

    @Mock LeagueRepository leagueRepository;
    @Mock LeagueMemberRepository memberRepository;
    @Mock ScoringConfigRepository configRepository;
    @Mock ScoringDirectory scoring;
    @Mock AuthDirectory auth;

    LeagueService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID adminLeagueId = UUID.randomUUID();
    private final UUID joinedLeagueId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new LeagueService(leagueRepository, memberRepository, configRepository, scoring, auth);
    }

    private League league(UUID id, String name, UUID adminUserId) {
        League l = new League();
        l.setId(id);
        l.setName(name);
        l.setVisibility("PRIVATE");
        l.setInviteCode("SECRET42");
        l.setAdminUserId(adminUserId);
        return l;
    }

    private LeagueMember membership(UUID leagueId) {
        LeagueMember m = new LeagueMember();
        m.setLeagueId(leagueId);
        m.setUserId(userId);
        return m;
    }

    @Test
    void returnsOnlyLeaguesTheUserBelongsTo() {
        when(memberRepository.findByUserId(userId))
            .thenReturn(List.of(membership(adminLeagueId), membership(joinedLeagueId)));
        when(leagueRepository.findAllById(List.of(adminLeagueId, joinedLeagueId)))
            .thenReturn(List.of(
                league(adminLeagueId, "Mine", userId),
                league(joinedLeagueId, "Theirs", UUID.randomUUID())));
        when(memberRepository.countByLeagueId(adminLeagueId)).thenReturn(5L);
        when(memberRepository.countByLeagueId(joinedLeagueId)).thenReturn(3L);

        List<LeagueResponse> leagues = service.listMyLeagues(userId);

        assertThat(leagues).extracting(LeagueResponse::name)
            .containsExactly("Mine", "Theirs");
        assertThat(leagues).extracting(LeagueResponse::memberCount)
            .containsExactly(5L, 3L);
    }

    @Test
    void exposesTheInviteCodeOnlyForLeaguesTheUserAdministers() {
        when(memberRepository.findByUserId(userId))
            .thenReturn(List.of(membership(adminLeagueId), membership(joinedLeagueId)));
        when(leagueRepository.findAllById(List.of(adminLeagueId, joinedLeagueId)))
            .thenReturn(List.of(
                league(adminLeagueId, "Mine", userId),
                league(joinedLeagueId, "Theirs", UUID.randomUUID())));

        List<LeagueResponse> leagues = service.listMyLeagues(userId);

        assertThat(leagues.get(0).inviteCode()).isEqualTo("SECRET42");
        assertThat(leagues.get(1).inviteCode()).isNull();
    }

    @Test
    void returnsEmptyWithoutQueryingLeaguesWhenUserHasNoMemberships() {
        when(memberRepository.findByUserId(userId)).thenReturn(List.of());

        assertThat(service.listMyLeagues(userId)).isEmpty();
    }
}
