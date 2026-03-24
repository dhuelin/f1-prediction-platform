package com.f1predict.f1data.repository;

import com.f1predict.f1data.model.RaceResult;
import com.f1predict.f1data.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RaceResultRepository extends JpaRepository<RaceResult, UUID> {
    List<RaceResult> findByRaceIdAndSessionType(String raceId, Session.SessionType sessionType);
    List<RaceResult> findByRaceId(String raceId);
}
