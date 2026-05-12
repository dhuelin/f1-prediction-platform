package com.f1predict.f1data.controller;

import com.f1predict.f1data.dto.openf1.OpenF1PositionDto;
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

    public LiveController(LiveSessionService liveSessionService, DriverRepository driverRepository) {
        this.liveSessionService = liveSessionService;
        this.driverRepository = driverRepository;
    }

    record DriverPositionEntry(int driverNumber, String driverCode, int position) {}
    record LivePositionsResponse(List<DriverPositionEntry> positions) {}

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
}
