package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.GameFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameFeedbackRepository extends JpaRepository<GameFeedback, Long> {
    List<GameFeedback> findByEventId(Long eventId);
}


