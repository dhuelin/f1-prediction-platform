package com.f1predict.notification.push;

import com.f1predict.notification.model.DeviceToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PushDispatcher {

    private final PushProvider apnsProvider;
    private final PushProvider fcmProvider;

    public PushDispatcher(PushProvider apnsProvider, PushProvider fcmProvider) {
        this.apnsProvider = apnsProvider;
        this.fcmProvider = fcmProvider;
    }

    public void dispatch(List<DeviceToken> tokens, PushPayload payload) {
        Map<DeviceToken.Platform, List<String>> byPlatform = tokens.stream()
            .collect(Collectors.groupingBy(
                DeviceToken::getPlatform,
                Collectors.mapping(DeviceToken::getToken, Collectors.toList())
            ));

        List<String> apnsTokens = byPlatform.getOrDefault(DeviceToken.Platform.APNS, List.of());
        List<String> fcmTokens  = byPlatform.getOrDefault(DeviceToken.Platform.FCM, List.of());

        if (!apnsTokens.isEmpty()) apnsProvider.send(apnsTokens, payload);
        if (!fcmTokens.isEmpty())  fcmProvider.send(fcmTokens, payload);
    }
}
