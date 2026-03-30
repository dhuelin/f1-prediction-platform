package com.f1predict.notification.repository;

import com.f1predict.notification.model.NotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, Long> {
    Optional<NotificationPreferences> findByUserId(UUID userId);
    List<NotificationPreferences> findByUserIdIn(Collection<UUID> userIds);
}
