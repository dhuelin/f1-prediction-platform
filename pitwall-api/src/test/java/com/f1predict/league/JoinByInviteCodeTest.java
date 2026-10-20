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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers POST /leagues/join.
 *
 * A user is given an invite code, never a league UUID, but the only join
 * endpoint keyed on the id: the web app passed the code where the id belonged
 * and the mobile app POSTed to /leagues/join, which did not exist. Joining a
 * private league by code therefore worked in neither client.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JoinByInviteCodeTest {

    @Mock LeagueRepository leagueRepository;
    @Mock LeagueMemberRepository memberRepository;
    @Mock ScoringConfigRepository configRepository;
    @Mock ScoringDirectory scoring;
    @Mock AuthDirectory auth;

    LeagueService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID leagueId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new LeagueService(leagueRepository, memberRepository, configRepository, scoring, auth);
    }

    private League privateLeague() {
        League l = new League();
        l.setId(leagueId);
        l.setName("Paddock Pundits");
        l.setVisibility("PRIVATE");
        l.setInviteCode("PUNDITS42");
        l.setAdminUserId(UUID.randomUUID());
        return l;
    }

    @Test
    void joinsTheLeagueOwningTheCode() {
        when(leagueRepository.findByInviteCode("PUNDITS42")).thenReturn(Optional.of(privateLeague()));
        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(privateLeague()));
        when(memberRepository.findByLeagueIdAndUserId(leagueId, userId)).thenReturn(Optional.empty());
        when(scoring.getLeagueAveragePoints(leagueId)).thenReturn(42);

        LeagueResponse response = service.joinByInviteCode(userId, "PUNDITS42");

        assertThat(response.id()).isEqualTo(leagueId);
        ArgumentCaptor<LeagueMember> saved = ArgumentCaptor.forClass(LeagueMember.class);
        verify(memberRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getLeagueId()).isEqualTo(leagueId);
        // Mid-season catch-up: a joiner starts on the league average, not zero
        assertThat(saved.getValue().getCatchUpPoints()).isEqualTo(42);
    }

    @Test
    void trimsSurroundingWhitespaceFromThePastedCode() {
        when(leagueRepository.findByInviteCode("PUNDITS42")).thenReturn(Optional.of(privateLeague()));
        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(privateLeague()));
        when(memberRepository.findByLeagueIdAndUserId(leagueId, userId)).thenReturn(Optional.empty());

        assertThat(service.joinByInviteCode(userId, "  PUNDITS42  ").id()).isEqualTo(leagueId);
    }

    @Test
    void unknownCodeIsNotFound() {
        when(leagueRepository.findByInviteCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.joinByInviteCode(userId, "NOPE"))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void blankCodeIsRejectedWithoutQuerying() {
        assertThatThrownBy(() -> service.joinByInviteCode(userId, "   "))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.joinByInviteCode(userId, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void joiningTwiceIsRejected() {
        when(leagueRepository.findByInviteCode("PUNDITS42")).thenReturn(Optional.of(privateLeague()));
        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(privateLeague()));
        when(memberRepository.findByLeagueIdAndUserId(leagueId, userId))
            .thenReturn(Optional.of(new LeagueMember()));

        assertThatThrownBy(() -> service.joinByInviteCode(userId, "PUNDITS42"))
            .isInstanceOf(IllegalStateException.class);
    }
}
