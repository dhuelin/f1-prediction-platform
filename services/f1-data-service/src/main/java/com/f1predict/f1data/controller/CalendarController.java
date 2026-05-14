package com.f1predict.f1data.controller;

import com.f1predict.f1data.model.Race;
import com.f1predict.f1data.repository.RaceRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/calendar")
public class CalendarController {

    private final RaceRepository raceRepository;

    public CalendarController(RaceRepository raceRepository) {
        this.raceRepository = raceRepository;
    }

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

    record CalendarResponse(int season, List<RaceResponse> races) {}

    private static String computeStatus(Race race) {
        if (race.getRaceDate() == null) return "upcoming";
        Instant now = Instant.now();
        if (race.getQualifyingDeadline() != null && race.getQualifyingDeadline().isAfter(now)) {
            return "upcoming";
        }
        if (race.getRaceDate().isAfter(now)) {
            return "upcoming";
        }
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

    /** GET /f1/calendar/current */
    @GetMapping("/current")
    public CalendarResponse getCurrentCalendar() {
        int season = LocalDate.now(ZoneOffset.UTC).getYear();
        List<RaceResponse> races = raceRepository.findBySeason(season).stream()
            .sorted(Comparator.comparingInt(Race::getRound))
            .map(CalendarController::toResponse)
            .toList();
        return new CalendarResponse(season, races);
    }

    /** GET /f1/calendar/{season} — fetch a specific season */
    @GetMapping("/{season}")
    public CalendarResponse getCalendarBySeason(@PathVariable int season) {
        List<RaceResponse> races = raceRepository.findBySeason(season).stream()
            .sorted(Comparator.comparingInt(Race::getRound))
            .map(CalendarController::toResponse)
            .toList();
        return new CalendarResponse(season, races);
    }
}
