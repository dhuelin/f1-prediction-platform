package com.f1predict.f1data.controller;

import com.f1predict.f1data.model.Race;
import com.f1predict.f1data.model.RaceResult;
import com.f1predict.f1data.model.Session;
import com.f1predict.f1data.repository.RaceRepository;
import com.f1predict.f1data.repository.RaceResultRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/races")
public class RaceController {

    private final RaceRepository raceRepository;
    private final RaceResultRepository raceResultRepository;

    public RaceController(RaceRepository raceRepository, RaceResultRepository raceResultRepository) {
        this.raceRepository = raceRepository;
        this.raceResultRepository = raceResultRepository;
    }

    // --- DTOs ---

    record DeadlineResponse(String raceId, Instant qualifyingDeadline) {}

    record RaceResponse(
        String id,
        String name,
        String circuitName,
        String country,
        String city,
        String raceDateTime,
        String qualifyingDateTime,
        int round,
        int season,
        String status,
        boolean sprintWeekend
    ) {}

    record RaceResultResponse(
        String raceId,
        int position,
        String driverId,
        String driverCode,
        String timeOrStatus,
        boolean fastestLap
    ) {}

    record CalendarResponse(int season, List<RaceResponse> races) {}

    // --- Helpers ---

    private static String computeStatus(Race race) {
        if (race.getRaceDate() == null) return "upcoming";
        Instant now = Instant.now();
        // Live window: qualifying deadline passed but race is within ~4 hours ago
        if (race.getQualifyingDeadline() != null && race.getQualifyingDeadline().isAfter(now)) {
            return "upcoming";
        }
        // Race day: within 4 hours either side of race start
        if (race.getRaceDate().isAfter(now)) {
            return "upcoming";
        }
        // Race started more than 4 hours ago → finished
        if (race.getRaceDate().plusSeconds(4 * 3600).isBefore(now)) {
            return "finished";
        }
        return "live";
    }

    private static RaceResponse toResponse(Race race) {
        return new RaceResponse(
            race.getId(),
            race.getRaceName(),
            race.getCircuitName(),
            race.getCountry(),
            race.getCity(),
            race.getRaceDate() != null ? race.getRaceDate().toString() : null,
            race.getQualifyingDeadline() != null ? race.getQualifyingDeadline().toString() : null,
            race.getRound(),
            race.getSeason(),
            computeStatus(race),
            race.isSprintWeekend()
        );
    }

    // --- Endpoints ---

    /** GET /f1/races/{raceId}/deadline */
    @GetMapping("/{raceId}/deadline")
    public ResponseEntity<DeadlineResponse> getDeadline(@PathVariable String raceId) {
        return raceRepository.findById(raceId)
            .map(race -> ResponseEntity.ok(new DeadlineResponse(raceId, race.getQualifyingDeadline())))
            .orElse(ResponseEntity.notFound().build());
    }

    /** GET /f1/races/next — upcoming race closest to now */
    @GetMapping("/next")
    public ResponseEntity<RaceResponse> getNextRace() {
        int season = LocalDate.now(ZoneOffset.UTC).getYear();
        Instant now = Instant.now();
        return raceRepository.findBySeason(season).stream()
            .filter(r -> r.getRaceDate() == null || r.getRaceDate().isAfter(now))
            .min(Comparator.comparing(r -> r.getRaceDate() != null ? r.getRaceDate() : Instant.MAX))
            .map(race -> ResponseEntity.ok(toResponse(race)))
            .orElse(ResponseEntity.notFound().build());
    }

    /** GET /f1/races/{raceId}/results?sessionType=RACE */
    @GetMapping("/{raceId}/results")
    public List<RaceResultResponse> getResults(
            @PathVariable String raceId,
            @RequestParam(defaultValue = "RACE") String sessionType) {
        Session.SessionType type;
        try {
            type = Session.SessionType.valueOf(sessionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = Session.SessionType.RACE;
        }
        return raceResultRepository.findByRaceIdAndSessionType(raceId, type).stream()
            .sorted(Comparator.comparingInt(r -> r.getFinishPosition() != null ? r.getFinishPosition() : 99))
            .map(r -> new RaceResultResponse(
                raceId,
                r.getFinishPosition() != null ? r.getFinishPosition() : 0,
                r.getDriverCode(),
                r.getDriverCode(),
                r.getStatus().name(),
                r.isFastestLap()
            ))
            .toList();
    }

}
