package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.GameFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

public interface GameFeedbackRepository extends JpaRepository<GameFeedback, Long> {
    List<GameFeedback> findByEventId(Long eventId);
    List<GameFeedback> findByGameId(String gameId);
    List<GameFeedback> findByGameIdOrderByCreatedAtDesc(String gameId);
    List<GameFeedback> findByGameIdAndCreatedAtAfter(String gameId, LocalDateTime date);
    List<GameFeedback> findByEventIdAndGameId(Long eventId, String gameId);
}


