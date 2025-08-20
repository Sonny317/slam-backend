package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.GameFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameFeedbackRepository extends JpaRepository<GameFeedback, Long> {
    List<GameFeedback> findByEventId(Long eventId);
    List<GameFeedback> findByGameId(String gameId);
    List<GameFeedback> findByGameIdOrderByCreatedAtDesc(String gameId);
    List<GameFeedback> findByGameIdAndCreatedAtAfter(String gameId, LocalDateTime date);
    List<GameFeedback> findByEventIdAndGameId(Long eventId, String gameId);
}


