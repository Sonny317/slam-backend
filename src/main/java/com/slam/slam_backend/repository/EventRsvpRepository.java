package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.EventRsvp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EventRsvpRepository extends JpaRepository<EventRsvp, Long> {
    // 특정 사용자와 특정 이벤트에 대한 RSVP 정보를 찾는 메소드
    Optional<EventRsvp> findByUser_IdAndEvent_Id(Long userId, Long eventId);
}