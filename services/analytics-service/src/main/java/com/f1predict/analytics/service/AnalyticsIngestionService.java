package com.f1predict.analytics.service;

import com.f1predict.analytics.model.PredictionParticipationStat;
import com.f1predict.analytics.repository.PredictionParticipationStatRepository;
import com.f1predict.common.events.PredictionLockedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsIngestionService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsIngestionService.class);

    private final PredictionParticipationStatRepository participationRepo;

    public AnalyticsIngestionService(PredictionParticipationStatRepository participationRepo) {
        this.participationRepo = participationRepo;
    }

    @Transactional
    public void recordPredictionLocked(PredictionLockedEvent event) {
        participationRepo.findByRaceId(event.raceId()).ifPresentOrElse(
            existing -> log.warn("analytics: PredictionParticipationStat already exists for race={}", event.raceId()),
            () -> {
                var stat = new PredictionParticipationStat(
                    event.raceId(),
                    event.lockedCount(),
                    event.sessionType() != null ? event.sessionType().name() : null
                );
                participationRepo.save(stat);
                log.info("analytics: recorded participation race={} locked={}", event.raceId(), event.lockedCount());
            }
        );
    }
}
