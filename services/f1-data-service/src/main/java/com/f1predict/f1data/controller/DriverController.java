package com.f1predict.f1data.controller;

import com.f1predict.f1data.repository.DriverRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/drivers")
public class DriverController {

    private final DriverRepository driverRepository;

    public DriverController(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    record DriverResponse(
        String id,
        String name,
        String code,
        String team,
        Integer number,
        String nationality,
        int season
    ) {}

    /** GET /f1/drivers?season=2026 (defaults to current year) */
    @GetMapping
    public List<DriverResponse> getDrivers(
            @RequestParam(required = false) Integer season) {
        int s = season != null ? season : LocalDate.now(ZoneOffset.UTC).getYear();
        return driverRepository.findBySeason(s).stream()
            .sorted(Comparator.comparingInt(d -> d.getDriverNumber() != null ? d.getDriverNumber() : 99))
            .map(d -> new DriverResponse(
                d.getCode(),
                d.getFirstName() + " " + d.getLastName(),
                d.getCode(),
                d.getConstructorName(),
                d.getDriverNumber(),
                d.getNationality(),
                d.getSeason()
            ))
            .toList();
    }
}
