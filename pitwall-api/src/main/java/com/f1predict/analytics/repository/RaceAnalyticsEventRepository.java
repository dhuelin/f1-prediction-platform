package com.f1predict.analytics.repository;

import com.f1predict.analytics.model.RaceAnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RaceAnalyticsEventRepository extends JpaRepository<RaceAnalyticsEvent, Long> {
    List<RaceAnalyticsEvent> findByRaceIdOrderByOccurredAtAsc(String raceId);
    List<RaceAnalyticsEvent> findByEventTypeOrderByOccurredAtDesc(String eventType);
}
