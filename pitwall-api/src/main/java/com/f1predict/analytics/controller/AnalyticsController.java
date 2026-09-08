package com.f1predict.analytics.controller;

import com.f1predict.analytics.model.PredictionParticipationStat;
import com.f1predict.analytics.model.RaceAnalyticsEvent;
import com.f1predict.analytics.repository.PredictionParticipationStatRepository;
import com.f1predict.analytics.repository.RaceAnalyticsEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final RaceAnalyticsEventRepository eventRepo;
    private final PredictionParticipationStatRepository participationRepo;

    public AnalyticsController(RaceAnalyticsEventRepository eventRepo,
                                PredictionParticipationStatRepository participationRepo) {
        this.eventRepo = eventRepo;
        this.participationRepo = participationRepo;
    }

    @GetMapping("/races/{raceId}/events")
    public List<EventResponse> getRaceEvents(@PathVariable String raceId) {
        return eventRepo.findByRaceIdOrderByOccurredAtAsc(raceId)
            .stream()
            .map(EventResponse::from)
            .toList();
    }

    @GetMapping("/races/{raceId}/participation")
    public ResponseEntity<ParticipationResponse> getParticipation(@PathVariable String raceId) {
        return participationRepo.findByRaceId(raceId)
            .map(ParticipationResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/participation")
    public List<ParticipationResponse> getAllParticipation() {
        return participationRepo.findAllByOrderByLockedAtDesc()
            .stream()
            .map(ParticipationResponse::from)
            .toList();
    }

    public record EventResponse(Long id, String eventType, String raceId, String leagueId, Instant occurredAt) {
        static EventResponse from(RaceAnalyticsEvent e) {
            return new EventResponse(e.getId(), e.getEventType(), e.getRaceId(), e.getLeagueId(), e.getOccurredAt());
        }
    }

    public record ParticipationResponse(String raceId, int lockedCount, String sessionType, Instant lockedAt) {
        static ParticipationResponse from(PredictionParticipationStat s) {
            return new ParticipationResponse(s.getRaceId(), s.getLockedCount(), s.getSessionType(), s.getLockedAt());
        }
    }
}
