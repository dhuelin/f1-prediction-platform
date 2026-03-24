package com.f1predict.f1data.service;

import com.f1predict.f1data.client.F1ApiException;
import com.f1predict.f1data.client.JolpicaClient;
import com.f1predict.f1data.dto.jolpica.JolpicaRaceDto;
import com.f1predict.f1data.model.Race;
import com.f1predict.f1data.model.Session;
import com.f1predict.f1data.repository.RaceRepository;
import com.f1predict.f1data.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class RaceCalendarService {

    private final JolpicaClient jolpicaClient;
    private final RaceRepository raceRepository;
    private final SessionRepository sessionRepository;

    public RaceCalendarService(JolpicaClient jolpicaClient,
                                RaceRepository raceRepository,
                                SessionRepository sessionRepository) {
        this.jolpicaClient = jolpicaClient;
        this.raceRepository = raceRepository;
        this.sessionRepository = sessionRepository;
    }

    public void syncCalendar(int season) {
        List<JolpicaRaceDto> raceDtos = jolpicaClient.fetchCalendar(season);
        for (JolpicaRaceDto dto : raceDtos) {
            Race race = upsertRace(season, dto);
            upsertSessions(race, dto);
        }
    }

    private Race upsertRace(int season, JolpicaRaceDto dto) {
        int round = parseRound(dto.round());
        String id = season + "-" + String.format("%02d", round);
        Race race = raceRepository.findBySeasonAndRound(season, round)
            .orElseGet(() -> {
                Race r = new Race();
                r.setId(id);
                return r;
            });

        race.setSeason(season);
        race.setRound(round);
        race.setRaceName(dto.raceName());
        race.setCircuitName(dto.circuit().circuitName());
        race.setCountry(dto.circuit().location().country());
        race.setRaceDate(parseInstant(dto.date(), dto.time()));
        race.setSprintWeekend(dto.isSprintWeekend());

        return raceRepository.save(race);
    }

    private int parseRound(String round) {
        try {
            return Integer.parseInt(round);
        } catch (NumberFormatException e) {
            throw new F1ApiException("Unparseable round value: " + round, e);
        }
    }

    private void upsertSessions(Race race, JolpicaRaceDto dto) {
        upsertSession(race, Session.SessionType.RACE, race.getRaceDate());

        if (dto.isSprintWeekend()) {
            JolpicaRaceDto.SprintInfo sprint = dto.sprint();
            Instant sprintAt = parseInstant(sprint.date(), sprint.time());
            upsertSession(race, Session.SessionType.SPRINT, sprintAt);
        }
    }

    private void upsertSession(Race race, Session.SessionType type, Instant scheduledAt) {
        List<Session> existing = sessionRepository.findByRaceIdAndSessionType(race.getId(), type);
        Session session = existing.isEmpty() ? new Session() : existing.get(0);
        session.setRace(race);
        session.setSessionType(type);
        session.setScheduledAt(scheduledAt);
        sessionRepository.save(session);
    }

    private Instant parseInstant(String date, String time) {
        try {
            String t = (time == null) ? "00:00:00Z" : time;
            if (!t.endsWith("Z")) t = t + "Z";
            return Instant.parse(date + "T" + t);
        } catch (Exception e) {
            throw new F1ApiException("Failed to parse date/time: date=" + date + " time=" + time, e);
        }
    }
}
