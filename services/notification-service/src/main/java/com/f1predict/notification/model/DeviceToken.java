package com.f1predict.notification.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_tokens")
public class DeviceToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Platform platform;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public enum Platform { APNS, FCM }

    protected DeviceToken() {}

    public DeviceToken(UUID userId, String token, Platform platform) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
    }

    public Long getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public Platform getPlatform() { return platform; }
}
