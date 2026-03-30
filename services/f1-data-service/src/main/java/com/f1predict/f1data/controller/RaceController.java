package com.f1predict.f1data.controller;

import com.f1predict.f1data.repository.RaceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/races")
public class RaceController {

    private final RaceRepository raceRepository;

    public RaceController(RaceRepository raceRepository) {
        this.raceRepository = raceRepository;
    }

    record DeadlineResponse(String raceId, Instant qualifyingDeadline) {}

    @GetMapping("/{raceId}/deadline")
    public ResponseEntity<DeadlineResponse> getDeadline(@PathVariable String raceId) {
        return raceRepository.findById(raceId)
            .map(race -> ResponseEntity.ok(new DeadlineResponse(raceId, race.getQualifyingDeadline())))
            .orElse(ResponseEntity.notFound().build());
    }
}
