package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.EventRsvp;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EventRsvpRepository extends JpaRepository<EventRsvp, Long> {
    // 특정 사용자와 특정 이벤트에 대한 RSVP 정보를 찾는 메소드
    Optional<EventRsvp> findByUser_IdAndEvent_Id(Long userId, Long eventId);

    // ✅ 추가: 특정 이벤트에 대한 모든 RSVP 기록을 삭제하는 메소드
    void deleteAllByEventId(Long eventId);
    
    // ✅ 추가: 특정 이벤트의 모든 RSVP 조회
    List<EventRsvp> findByEvent_Id(Long eventId);
    
    // ✅ 추가: 특정 이벤트의 참석자만 조회
    List<EventRsvp> findByEvent_IdAndIsAttendingTrue(Long eventId);
    
    // ✅ 추가: 특정 이벤트의 애프터파티 참석자만 조회
    List<EventRsvp> findByEvent_IdAndAfterPartyTrue(Long eventId);
}