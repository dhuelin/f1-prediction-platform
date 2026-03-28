package com.f1predict.prediction.controller;

import com.f1predict.prediction.dto.BonusBetRequest;
import com.f1predict.prediction.dto.BonusBetResponse;
import com.f1predict.prediction.dto.PredictionResponse;
import com.f1predict.prediction.dto.SubmitPredictionRequest;
import com.f1predict.prediction.service.PredictionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/predictions")
@Validated
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping("/{raceId}")
    @ResponseStatus(HttpStatus.CREATED)
    public PredictionResponse submit(
            @PathVariable @Pattern(regexp = "^[A-Z0-9_-]{3,20}$") String raceId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody SubmitPredictionRequest request) {
        return predictionService.submitPrediction(userId, raceId, request);
    }

    @PutMapping("/{raceId}")
    public PredictionResponse update(
            @PathVariable @Pattern(regexp = "^[A-Z0-9_-]{3,20}$") String raceId,
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody SubmitPredictionRequest request) {
        return predictionService.updatePrediction(userId, raceId, request);
    }

    @PostMapping("/{raceId}/bets")
    @ResponseStatus(HttpStatus.CREATED)
    public BonusBetResponse submitBet(
            @PathVariable @Pattern(regexp = "^[A-Z0-9_-]{3,20}$") String raceId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "RACE") @Pattern(regexp = "RACE|SPRINT") String sessionType,
            @RequestParam(required = false) UUID leagueId,
            @Valid @RequestBody BonusBetRequest request) {
        return predictionService.submitBet(userId, raceId, sessionType, leagueId, request);
    }
}
