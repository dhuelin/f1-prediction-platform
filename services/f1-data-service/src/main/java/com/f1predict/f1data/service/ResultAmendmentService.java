package com.f1predict.f1data.service;

import com.f1predict.common.events.*;
import com.f1predict.f1data.client.F1ApiException;
import com.f1predict.f1data.client.JolpicaClient;
import com.f1predict.f1data.dto.jolpica.JolpicaResultDto;
import com.f1predict.f1data.model.Race;
import com.f1predict.f1data.model.RaceResult;
import com.f1predict.f1data.model.Session;
import com.f1predict.f1data.publisher.F1EventPublisher;
import com.f1predict.f1data.repository.RaceRepository;
import com.f1predict.f1data.repository.RaceResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ResultAmendmentService {

    private static final Logger log = LoggerFactory.getLogger(ResultAmendmentService.class);

    private final JolpicaClient jolpicaClient;
    private final RaceRepository raceRepository;
    private final RaceResultRepository raceResultRepository;
    private final F1EventPublisher eventPublisher;

    public ResultAmendmentService(JolpicaClient jolpicaClient,
                                   RaceRepository raceRepository,
                                   RaceResultRepository raceResultRepository,
                                   F1EventPublisher eventPublisher) {
        this.jolpicaClient = jolpicaClient;
        this.raceRepository = raceRepository;
        this.raceResultRepository = raceResultRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void checkForAmendments() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(48, ChronoUnit.HOURS);
        List<Race> recentRaces = raceRepository.findByRaceDateBetween(cutoff, now);

        for (Race race : recentRaces) {
            try {
                checkRaceForAmendments(race);
            } catch (F1ApiException e) {
                log.warn("Failed to fetch amendment data for race {}: {}", race.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    private void checkRaceForAmendments(Race race) {
        List<JolpicaResultDto> latest = jolpicaClient.fetchRaceResults(race.getSeason(), race.getRound());
        if (latest.isEmpty()) return;

        List<RaceResult> stored = raceResultRepository.findByRaceIdAndSessionType(
            race.getId(), Session.SessionType.RACE);

        Map<String, RaceResult> storedByDriver = new HashMap<>();
        for (RaceResult r : stored) storedByDriver.put(r.getDriverCode(), r);

        Map<String, List<RaceResultFinalEvent.DriverResult>> byReason = new LinkedHashMap<>();

        for (JolpicaResultDto dto : latest) {
            String code = dto.driver().code();
            Integer newPosition = parsePosition(dto.position());
            RaceResult.DriverStatus newStatus = mapStatus(dto.status());

            RaceResult stored_ = storedByDriver.get(code);
            if (stored_ == null) continue;

            boolean positionChanged = !Objects.equals(stored_.getFinishPosition(), newPosition);
            boolean statusChanged = stored_.getStatus() != newStatus;

            if (positionChanged || statusChanged) {
                stored_.setFinishPosition(newPosition);
                stored_.setStatus(newStatus);
                stored_.setAmendedAt(Instant.now());
                raceResultRepository.save(stored_);

                String reason = (newStatus == RaceResult.DriverStatus.DSQ)
                    ? "POST_RACE_DSQ"
                    : "POST_RACE_TIME_PENALTY";

                byReason.computeIfAbsent(reason, k -> new ArrayList<>())
                    .add(new RaceResultFinalEvent.DriverResult(
                        code, newPosition != null ? newPosition : 0,
                        RaceResultFinalEvent.DriverStatus.valueOf(newStatus.name())));
            }
        }

        for (Map.Entry<String, List<RaceResultFinalEvent.DriverResult>> entry : byReason.entrySet()) {
            eventPublisher.publishResultAmended(new ResultAmendedEvent(
                race.getId(),
                SessionCompleteEvent.SessionType.RACE,
                entry.getValue(),
                entry.getKey()));
        }
    }

    private Integer parsePosition(String position) {
        if (position == null) return null;
        try { return Integer.parseInt(position); } catch (NumberFormatException e) { return null; }
    }

    private RaceResult.DriverStatus mapStatus(String status) {
        if (status == null) return RaceResult.DriverStatus.DNF;
        return switch (status.toUpperCase()) {
            case "DISQUALIFIED" -> RaceResult.DriverStatus.DSQ;
            case "WITHDRAWN" -> RaceResult.DriverStatus.DNS;
            case "FINISHED" -> RaceResult.DriverStatus.CLASSIFIED;
            default -> RaceResult.DriverStatus.DNF;
        };
    }
}
