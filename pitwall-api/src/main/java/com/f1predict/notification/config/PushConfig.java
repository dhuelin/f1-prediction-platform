package com.f1predict.notification.config;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.f1predict.notification.push.ApnsPushProvider;
import com.f1predict.notification.push.FcmPushProvider;
import com.f1predict.notification.push.PushProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class PushConfig {

    private static final Logger log = LoggerFactory.getLogger(PushConfig.class);

    @Value("${apns.team-id:}") private String apnsTeamId;
    @Value("${apns.key-id:}") private String apnsKeyId;
    @Value("${apns.bundle-id:com.f1predict.app}") private String apnsBundleId;
    @Value("${apns.auth-key:}") private String apnsAuthKey;
    @Value("${apns.production:false}") private boolean apnsProduction;
    @Value("${fcm.credentials-json:}") private String fcmCredentialsJson;

    @Bean(name = "apnsProvider")
    public PushProvider apnsProvider() {
        if (apnsTeamId.isBlank() || apnsKeyId.isBlank() || apnsAuthKey.isBlank()) {
            log.warn("APNs credentials not configured — APNs push notifications disabled");
            return (tokens, payload) ->
                log.info("[APNS no-op] Would send '{}' to {} token(s)", payload.title(), tokens.size());
        }
        try {
            ApnsClient client = new ApnsClientBuilder()
                .setApnsServer(apnsProduction
                    ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                    : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                .setSigningKey(
                    com.eatthepath.pushy.apns.auth.ApnsSigningKey.loadFromInputStream(
                        new ByteArrayInputStream(apnsAuthKey.getBytes(StandardCharsets.UTF_8)),
                        apnsTeamId, apnsKeyId))
                .build();
            return new ApnsPushProvider(client, apnsBundleId);
        } catch (Exception e) {
            log.error("Failed to initialise APNs client — falling back to no-op", e);
            return (tokens, payload) -> {};
        }
    }

    @Bean(name = "fcmProvider")
    public PushProvider fcmProvider() {
        if (fcmCredentialsJson.isBlank()) {
            log.warn("Firebase credentials not configured — FCM push notifications disabled");
            return (tokens, payload) ->
                log.info("[FCM no-op] Would send '{}' to {} token(s)", payload.title(), tokens.size());
        }
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(fcmCredentialsJson.getBytes(StandardCharsets.UTF_8)));
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
            FirebaseApp app;
            try {
                app = FirebaseApp.initializeApp(options);
            } catch (IllegalStateException alreadyInit) {
                app = FirebaseApp.getInstance();
            }
            return new FcmPushProvider(FirebaseMessaging.getInstance(app));
        } catch (Exception e) {
            log.error("Failed to initialise FCM client — falling back to no-op", e);
            return (tokens, payload) -> {};
        }
    }
}
