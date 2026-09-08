package com.f1predict.notification.service;

import com.f1predict.notification.model.DeviceToken;
import com.f1predict.notification.model.NotificationPreferences;
import com.f1predict.notification.push.PushDispatcher;
import com.f1predict.notification.push.PushPayload;
import com.f1predict.notification.repository.DeviceTokenRepository;
import com.f1predict.notification.repository.NotificationPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final DeviceTokenRepository tokenRepository;
    private final NotificationPreferencesRepository prefsRepository;
    private final PushDispatcher pushDispatcher;

    public NotificationService(DeviceTokenRepository tokenRepository,
                               NotificationPreferencesRepository prefsRepository,
                               PushDispatcher pushDispatcher) {
        this.tokenRepository = tokenRepository;
        this.prefsRepository = prefsRepository;
        this.pushDispatcher = pushDispatcher;
    }

    public void sendPredictionReminder(String raceId) {
        List<DeviceToken> tokens = getTokensForPref(NotificationPreferences::isPredictionReminder);
        if (tokens.isEmpty()) return;
        log.info("Sending prediction reminder for race {} to {} token(s)", raceId, tokens.size());
        pushDispatcher.dispatch(tokens, new PushPayload(
            "Predictions locked",
            "Predictions are now locked for race " + raceId + ". Good luck!",
            Map.of("type", "PREDICTION_REMINDER", "raceId", raceId)
        ));
    }

    public void sendRaceStartAlert(String raceId) {
        List<DeviceToken> tokens = getTokensForPref(NotificationPreferences::isRaceStart);
        if (tokens.isEmpty()) return;
        log.info("Sending race start alert for race {} to {} token(s)", raceId, tokens.size());
        pushDispatcher.dispatch(tokens, new PushPayload(
            "Race day!",
            "Race " + raceId + " is starting. Check your predictions!",
            Map.of("type", "RACE_START", "raceId", raceId)
        ));
    }

    public void sendResultsPublished(String raceId) {
        List<DeviceToken> tokens = getTokensForPref(NotificationPreferences::isResultsPublished);
        if (tokens.isEmpty()) return;
        log.info("Sending results published for race {} to {} token(s)", raceId, tokens.size());
        pushDispatcher.dispatch(tokens, new PushPayload(
            "Results are in!",
            "Final results for race " + raceId + " are published. See how you scored.",
            Map.of("type", "RESULTS_PUBLISHED", "raceId", raceId)
        ));
    }

    public void sendScoreAmended(String raceId, String reason) {
        List<DeviceToken> tokens = getTokensForPref(NotificationPreferences::isScoreAmended);
        if (tokens.isEmpty()) return;
        log.info("Sending score amended for race {} reason={} to {} token(s)", raceId, reason, tokens.size());
        pushDispatcher.dispatch(tokens, new PushPayload(
            "Scores updated",
            "Race " + raceId + " results were amended (" + reason + "). Your score may have changed.",
            Map.of("type", "SCORE_AMENDED", "raceId", raceId)
        ));
    }

    private List<DeviceToken> getTokensForPref(Predicate<NotificationPreferences> prefCheck) {
        List<DeviceToken> allTokens = tokenRepository.findAll();
        if (allTokens.isEmpty()) return List.of();

        Set<UUID> userIds = allTokens.stream()
            .map(DeviceToken::getUserId)
            .collect(Collectors.toSet());

        Map<UUID, NotificationPreferences> prefsByUser = prefsRepository.findByUserIdIn(userIds)
            .stream()
            .collect(Collectors.toMap(NotificationPreferences::getUserId, p -> p));

        return allTokens.stream()
            .filter(token -> {
                NotificationPreferences prefs = prefsByUser.get(token.getUserId());
                return prefs == null || prefCheck.test(prefs); // default: enabled when no row
            })
            .toList();
    }
}
