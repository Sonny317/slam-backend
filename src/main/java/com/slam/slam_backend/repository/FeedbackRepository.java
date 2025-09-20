package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByEventId(Long eventId);
    
    @Query("SELECT DISTINCT f.eventId FROM Feedback f")
    List<Long> findDistinctEventIds();
}


