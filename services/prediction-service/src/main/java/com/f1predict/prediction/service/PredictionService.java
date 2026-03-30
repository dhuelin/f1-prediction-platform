package com.f1predict.prediction.service;

import com.f1predict.prediction.client.F1DataClient;
import com.f1predict.prediction.client.ScoringClient;
import com.f1predict.prediction.dto.BonusBetRequest;
import com.f1predict.prediction.dto.BonusBetResponse;
import com.f1predict.prediction.dto.InternalPredictionResponse;
import com.f1predict.prediction.dto.PredictionResponse;
import com.f1predict.prediction.dto.SubmitPredictionRequest;
import com.f1predict.prediction.model.BonusBet;
import com.f1predict.prediction.model.Prediction;
import com.f1predict.prediction.model.PredictionEntry;
import com.f1predict.prediction.repository.BonusBetRepository;
import com.f1predict.prediction.repository.PredictionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final BonusBetRepository bonusBetRepository;
    private final ScoringClient scoringClient;
    private final F1DataClient f1DataClient;

    public PredictionService(PredictionRepository predictionRepository,
                             BonusBetRepository bonusBetRepository,
                             ScoringClient scoringClient,
                             F1DataClient f1DataClient) {
        this.predictionRepository = predictionRepository;
        this.bonusBetRepository = bonusBetRepository;
        this.scoringClient = scoringClient;
        this.f1DataClient = f1DataClient;
    }

    private void checkDeadline(String raceId) {
        Instant deadline = f1DataClient.getQualifyingDeadline(raceId);
        if (deadline != null && Instant.now().isAfter(deadline)) {
            throw new IllegalStateException("Prediction deadline has passed");
        }
    }

    @Transactional
    public PredictionResponse submitPrediction(UUID userId, String raceId, SubmitPredictionRequest req) {
        String sessionType = req.sessionType() != null ? req.sessionType() : "RACE";

        checkDeadline(raceId);

        predictionRepository.findByUserIdAndRaceIdAndSessionType(userId, raceId, sessionType)
            .ifPresent(p -> { throw new IllegalStateException("Prediction already submitted"); });

        Prediction prediction = new Prediction();
        prediction.setUserId(userId);
        prediction.setRaceId(raceId);
        prediction.setSessionType(sessionType);
        prediction.setLocked(false);

        List<PredictionEntry> entries = buildEntries(req.rankedDriverCodes(), prediction);
        prediction.setEntries(entries);

        Prediction saved = predictionRepository.save(prediction);
        return toResponse(saved);
    }

    @Transactional
    public PredictionResponse updatePrediction(UUID userId, String raceId, SubmitPredictionRequest req) {
        String sessionType = req.sessionType() != null ? req.sessionType() : "RACE";

        checkDeadline(raceId);

        Prediction prediction = predictionRepository
            .findByUserIdAndRaceIdAndSessionType(userId, raceId, sessionType)
            .orElseThrow(() -> new NoSuchElementException("No prediction found for this race"));

        if (prediction.isLocked()) {
            throw new IllegalStateException("Prediction is locked");
        }

        prediction.getEntries().clear();
        List<PredictionEntry> entries = buildEntries(req.rankedDriverCodes(), prediction);
        prediction.getEntries().addAll(entries);
        prediction.setUpdatedAt(Instant.now());

        Prediction saved = predictionRepository.save(prediction);
        return toResponse(saved);
    }

    @Transactional
    public BonusBetResponse submitBet(UUID userId, String raceId, String sessionType, UUID leagueId, BonusBetRequest req) {
        String sType = sessionType != null ? sessionType : "RACE";
        checkDeadline(raceId);
        Prediction prediction = predictionRepository
            .findByUserIdAndRaceIdAndSessionType(userId, raceId, sType)
            .orElseThrow(() -> new NoSuchElementException("No prediction found for this race"));
        if (prediction.isLocked()) {
            throw new IllegalStateException("Prediction is locked");
        }
        // Validate stake inside the transaction to minimise the TOCTOU window
        if (leagueId != null) {
            int balance = scoringClient.getBalance(userId, leagueId);
            if (req.stake() > balance) {
                throw new IllegalArgumentException("Stake exceeds available points balance");
            }
        }
        BonusBet bet = new BonusBet();
        bet.setPrediction(prediction);
        bet.setBetType(req.betType());
        bet.setStake(req.stake());
        bet.setBetValue(req.betValue());
        BonusBet saved = bonusBetRepository.save(bet);
        return new BonusBetResponse(saved.getId(), saved.getBetType(), saved.getStake(), saved.getBetValue());
    }

    public List<InternalPredictionResponse> getLockedPredictions(String raceId, String sessionType) {
        return predictionRepository.findByRaceIdAndSessionTypeAndLockedTrue(raceId, sessionType)
            .stream()
            .map(p -> new InternalPredictionResponse(
                p.getUserId(),
                p.getSessionType(),
                p.getEntries().stream()
                    .sorted(java.util.Comparator.comparingInt(PredictionEntry::getPosition))
                    .map(PredictionEntry::getDriverCode)
                    .toList(),
                p.getBonusBets().stream()
                    .map(b -> new InternalPredictionResponse.InternalBetData(
                        b.getBetType().name(), b.getStake(), b.getBetValue()))
                    .toList(),
                p.getUpdatedAt()
            ))
            .toList();
    }

    private List<PredictionEntry> buildEntries(List<String> driverCodes, Prediction prediction) {
        return IntStream.range(0, driverCodes.size())
            .mapToObj(i -> {
                PredictionEntry entry = new PredictionEntry();
                entry.setPrediction(prediction);
                entry.setPosition(i + 1);
                entry.setDriverCode(driverCodes.get(i));
                return entry;
            })
            .toList();
    }

    private PredictionResponse toResponse(Prediction prediction) {
        List<String> driverCodes = prediction.getEntries().stream()
            .sorted(java.util.Comparator.comparingInt(PredictionEntry::getPosition))
            .map(PredictionEntry::getDriverCode)
            .toList();
        return new PredictionResponse(
            prediction.getId(),
            prediction.getUserId(),
            prediction.getRaceId(),
            prediction.getSessionType(),
            prediction.isLocked(),
            driverCodes
        );
    }
}
