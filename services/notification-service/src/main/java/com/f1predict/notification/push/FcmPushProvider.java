package com.f1predict.notification.push;

import com.google.api.core.ApiFutures;
import com.google.api.core.ApiFutureCallback;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FcmPushProvider implements PushProvider {

    private static final Logger log = LoggerFactory.getLogger(FcmPushProvider.class);
    private static final Executor CALLBACK_EXECUTOR = Executors.newCachedThreadPool();

    private final FirebaseMessaging messaging;

    public FcmPushProvider(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }

    @Override
    public void send(List<String> tokens, PushPayload payload) {
        Notification notification = Notification.builder()
            .setTitle(payload.title())
            .setBody(payload.body())
            .build();

        for (String token : tokens) {
            Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putAllData(payload.data())
                .build();
            ApiFutures.addCallback(messaging.sendAsync(message), new ApiFutureCallback<>() {
                @Override
                public void onSuccess(String msgId) {
                    // fire-and-forget — success is silent
                }

                @Override
                public void onFailure(Throwable ex) {
                    log.error("FCM send failed for token {}", token, ex);
                }
            }, CALLBACK_EXECUTOR);
        }
    }
}
