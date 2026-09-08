package com.f1predict.notification.push;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ApnsPushProvider implements PushProvider {

    private static final Logger log = LoggerFactory.getLogger(ApnsPushProvider.class);

    private final ApnsClient client;
    private final String bundleId;

    public ApnsPushProvider(ApnsClient client, String bundleId) {
        this.client = client;
        this.bundleId = bundleId;
    }

    @Override
    public void send(List<String> tokens, PushPayload payload) {
        String apnsPayload = new SimpleApnsPayloadBuilder()
            .setAlertTitle(payload.title())
            .setAlertBody(payload.body())
            .build();

        for (String token : tokens) {
            SimpleApnsPushNotification notification =
                new SimpleApnsPushNotification(token, bundleId, apnsPayload);
            client.sendNotification(notification).whenComplete((response, ex) -> {
                if (ex != null) {
                    log.error("APNs send failed for token {}", token, ex);
                } else if (!response.isAccepted()) {
                    log.warn("APNs rejected token {}: {}", token,
                        response.getRejectionReason().orElse("unknown"));
                }
            });
        }
    }
}
