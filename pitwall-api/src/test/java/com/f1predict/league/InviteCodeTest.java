package com.f1predict.league;

import com.f1predict.auth.api.AuthDirectory;
import com.f1predict.league.dto.CreateLeagueRequest;
import com.f1predict.league.model.League;
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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers invite-code generation.
 *
 * POST /leagues/join resolves a league from its code alone, so the code is the
 * whole secret guarding a private league. The previous generator took 8 hex chars
 * from a UUID — 32 bits — and nothing stopped two leagues sharing one.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InviteCodeTest {

    /** Upper-case alphanumerics minus the confusable glyphs I, O, 0 and 1. */
    private static final Pattern CODE = Pattern.compile("^[A-HJ-NP-Z2-9]{10}$");

    @Mock LeagueRepository leagueRepository;
    @Mock LeagueMemberRepository memberRepository;
    @Mock ScoringConfigRepository configRepository;
    @Mock ScoringDirectory scoring;
    @Mock AuthDirectory auth;

    LeagueService service;

    private final UUID adminId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new LeagueService(leagueRepository, memberRepository, configRepository, scoring, auth);
        when(leagueRepository.findByName(any())).thenReturn(Optional.empty());
        when(leagueRepository.findByInviteCode(any())).thenReturn(Optional.empty());
        when(leagueRepository.save(any())).thenAnswer(inv -> {
            League l = inv.getArgument(0);
            if (l.getId() == null) l.setId(UUID.randomUUID());
            return l;
        });
    }

    /** Captures every League handed to save(), newest last. */
    private League lastSavedLeague() {
        ArgumentCaptor<League> captor = ArgumentCaptor.forClass(League.class);
        verify(leagueRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private String createPrivateLeagueAndCaptureCode(String name) {
        service.createLeague(adminId, new CreateLeagueRequest(name, "PRIVATE"));
        return lastSavedLeague().getInviteCode();
    }

    @Test
    void privateLeagueCodeUsesTheFullColumnWidthAndAnUnambiguousAlphabet() {
        String code = createPrivateLeagueAndCaptureCode("Paddock Pundits");

        assertThat(code).matches(CODE);
        // 10 chars over a 32-symbol alphabet is 50 bits, against 32 before.
        assertThat(code).hasSize(10);
        assertThat(code).doesNotContain("I").doesNotContain("O").doesNotContain("0").doesNotContain("1");
    }

    @Test
    void publicLeagueGetsNoCode() {
        service.createLeague(adminId, new CreateLeagueRequest("Open House", "PUBLIC"));

        // The schema's chk_invite_code requires NULL for PUBLIC leagues.
        assertThat(lastSavedLeague().getInviteCode()).isNull();
    }

    @Test
    void codesDoNotRepeatAcrossManyLeagues() {
        for (int i = 0; i < 200; i++) {
            service.createLeague(adminId, new CreateLeagueRequest("League " + i, "PRIVATE"));
        }

        ArgumentCaptor<League> captor = ArgumentCaptor.forClass(League.class);
        verify(leagueRepository, atLeastOnce()).save(captor.capture());
        Set<String> seen = new HashSet<>();
        for (League l : captor.getAllValues()) {
            assertThat(l.getInviteCode()).matches(CODE);
            seen.add(l.getInviteCode());
        }
        assertThat(seen).hasSize(200);
    }

    @Test
    void retriesWhenTheDrawnCodeIsAlreadyTaken() {
        League existing = new League();
        existing.setId(UUID.randomUUID());
        // First lookup reports the code as taken, second reports it free.
        when(leagueRepository.findByInviteCode(any()))
            .thenReturn(Optional.of(existing))
            .thenReturn(Optional.empty());

        String code = createPrivateLeagueAndCaptureCode("Retry League");

        assertThat(code).matches(CODE);
    }

    @Test
    void failsRatherThanIssuingADuplicateWhenEveryAttemptCollides() {
        League existing = new League();
        existing.setId(UUID.randomUUID());
        when(leagueRepository.findByInviteCode(any())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
            service.createLeague(adminId, new CreateLeagueRequest("Doomed", "PRIVATE")))
            .isInstanceOf(IllegalStateException.class);
    }
}
