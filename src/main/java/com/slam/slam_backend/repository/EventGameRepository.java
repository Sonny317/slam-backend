package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.EventGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface EventGameRepository extends JpaRepository<EventGame, Long> {
    List<EventGame> findByEventId(Long eventId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM EventGame eg WHERE eg.eventId = :eventId")
    void deleteByEventId(Long eventId);
}
