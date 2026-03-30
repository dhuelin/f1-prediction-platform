package com.f1predict.notification.push;

import java.util.Map;

public record PushPayload(
    String title,
    String body,
    Map<String, String> data
) {}
