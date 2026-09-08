package com.f1predict.notification.push;

import java.util.List;

public interface PushProvider {
    /** Send a push notification to one or more device tokens. Fire-and-forget. */
    void send(List<String> tokens, PushPayload payload);
}
