package com.f1predict.prediction.service;

import com.f1predict.common.events.SessionCompleteEvent;
import com.f1predict.common.events.SessionCompleteEvent.SessionType;
import com.f1predict.prediction.config.RabbitMQConfig;
import com.f1predict.prediction.model.Prediction;
import com.f1predict.prediction.publisher.PredictionEventPublisher;
import com.f1predict.prediction.repository.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SessionCompleteListener {

    private static final Logger log = LoggerFactory.getLogger(SessionCompleteListener.class);

    private final PredictionRepository predictionRepository;
    private final PredictionEventPublisher eventPublisher;

    public SessionCompleteListener(PredictionRepository predictionRepository,
                                    PredictionEventPublisher eventPublisher) {
        this.predictionRepository = predictionRepository;
        this.eventPublisher = eventPublisher;
    }

    @RabbitListener(queues = RabbitMQConfig.SESSION_COMPLETE_QUEUE)
    @Transactional
    public void onSessionComplete(SessionCompleteEvent event) {
        // Lock main race predictions on QUALIFYING; sprint predictions on SPRINT_SHOOTOUT
        String sessionType;
        if (event.sessionType() == SessionType.QUALIFYING) {
            sessionType = "RACE";
        } else if (event.sessionType() == SessionType.SPRINT_SHOOTOUT) {
            sessionType = "SPRINT";
        } else {
            return; // SPRINT and RACE events don't trigger a lock
        }

        List<Prediction> predictions = predictionRepository
            .findByRaceIdAndSessionType(event.raceId(), sessionType);

        predictions.forEach(p -> p.setLocked(true));
        predictionRepository.saveAll(predictions);

        log.info("Locked {} {} predictions for race {}", predictions.size(), sessionType, event.raceId());

        eventPublisher.publishPredictionLocked(event.raceId(), event.sessionType(), predictions.size());
    }
}
