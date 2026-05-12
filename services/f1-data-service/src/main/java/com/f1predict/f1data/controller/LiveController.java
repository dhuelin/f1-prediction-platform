package com.f1predict.f1data.controller;

import com.f1predict.f1data.client.OpenF1Client;
import com.f1predict.f1data.dto.openf1.OpenF1LapDto;
import com.f1predict.f1data.dto.openf1.OpenF1PositionDto;
import com.f1predict.f1data.dto.openf1.OpenF1RaceControlDto;
import com.f1predict.f1data.model.Driver;
import com.f1predict.f1data.repository.DriverRepository;
import com.f1predict.f1data.service.LiveSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/live")
public class LiveController {

    private final LiveSessionService liveSessionService;
    private final DriverRepository driverRepository;
    private final OpenF1Client openF1Client;

    public LiveController(LiveSessionService liveSessionService,
                          DriverRepository driverRepository,
                          OpenF1Client openF1Client) {
        this.liveSessionService = liveSessionService;
        this.driverRepository = driverRepository;
        this.openF1Client = openF1Client;
    }

    record DriverPositionEntry(int driverNumber, String driverCode, int position) {}
    record LivePositionsResponse(List<DriverPositionEntry> positions) {}
    record RaceStateResponse(int scCount, int vscCount, String fastestLapDriverCode,
                              Double fastestLapDuration, List<String> dnfDriverCodes) {}

    @GetMapping("/positions")
    public ResponseEntity<LivePositionsResponse> getCurrentPositions() {
        List<OpenF1PositionDto> raw = liveSessionService.fetchCurrentPositions();
        if (raw.isEmpty()) {
            return ResponseEntity.ok(new LivePositionsResponse(List.of()));
        }

        Map<Integer, String> driverCodeByNumber = driverRepository
                .findBySeason(Year.now().getValue())
                .stream()
                .collect(Collectors.toMap(Driver::getDriverNumber, Driver::getCode));

        List<DriverPositionEntry> entries = raw.stream()
                .map(p -> new DriverPositionEntry(
                        p.driverNumber(),
                        driverCodeByNumber.getOrDefault(p.driverNumber(), "UNK"),
                        p.position()))
                .sorted((a, b) -> Integer.compare(a.position(), b.position()))
                .toList();

        return ResponseEntity.ok(new LivePositionsResponse(entries));
    }

    /**
     * Returns live bonus bet state for the active session:
     * SC/VSC deployment count, current fastest lap holder, DNF drivers.
     * Returns empty state when no session is active.
     */
    @GetMapping("/race-state")
    public ResponseEntity<RaceStateResponse> getRaceState() {
        int sessionKey = liveSessionService.getActiveSessionKey();
        if (sessionKey == 0) {
            return ResponseEntity.ok(new RaceStateResponse(0, 0, null, null, List.of()));
        }

        Map<Integer, String> driverCodeByNumber = driverRepository
                .findBySeason(Year.now().getValue())
                .stream()
                .collect(Collectors.toMap(Driver::getDriverNumber, Driver::getCode));

        // SC / VSC count from race control messages
        List<OpenF1RaceControlDto> rcMessages;
        try {
            rcMessages = openF1Client.fetchRaceControlMessages(sessionKey);
        } catch (Exception e) {
            rcMessages = List.of();
        }
        int scCount = (int) rcMessages.stream()
                .filter(m -> m.flag() != null && m.flag().equalsIgnoreCase("SAFETY_CAR"))
                .filter(m -> m.message() != null && m.message().toUpperCase().contains("DEPLOYED"))
                .count();
        int vscCount = (int) rcMessages.stream()
                .filter(m -> m.flag() != null && m.flag().equalsIgnoreCase("VIRTUAL_SAFETY_CAR"))
                .filter(m -> m.message() != null && m.message().toUpperCase().contains("DEPLOYED"))
                .count();

        // Fastest lap holder from laps data
        String fastestLapCode = null;
        Double fastestLapTime = null;
        try {
            List<OpenF1LapDto> laps = openF1Client.fetchLaps(sessionKey);
            var fastestLap = laps.stream()
                    .filter(l -> Boolean.TRUE.equals(l.isFastestLap()) && l.driverNumber() != null)
                    .findFirst();
            if (fastestLap.isPresent()) {
                fastestLapCode = driverCodeByNumber.getOrDefault(fastestLap.get().driverNumber(), "UNK");
                fastestLapTime = fastestLap.get().lapDuration();
            }
        } catch (Exception ignored) {}

        return ResponseEntity.ok(new RaceStateResponse(scCount, vscCount, fastestLapCode, fastestLapTime, List.of()));
    }
}
