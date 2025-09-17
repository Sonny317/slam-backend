package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.EventRsvp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // ✅ 추가: 체크인 완료(Attended)한 사용자들 조회
    List<EventRsvp> findByEvent_IdAndAttendedTrue(Long eventId);

    // ✅ 추가: 특정 사용자가 체크인 완료한 이벤트 수(참여 횟수) 카운트
    long countByUser_IdAndAttendedTrue(Long userId);
    
    // ✅ 추가: 특정 사용자와 이벤트의 RSVP 존재 여부 확인
    boolean existsByUser_IdAndEvent_Id(Long userId, Long eventId);
    
    // ✅ 추가: 특정 이벤트의 모든 RSVP를 User와 UserProfile과 함께 조회
    @Query("SELECT r FROM EventRsvp r " +
           "LEFT JOIN FETCH r.user u " +
           "LEFT JOIN FETCH u.userProfile " +
           "WHERE r.event.id = :eventId")
    List<EventRsvp> findByEvent_IdWithUserProfile(@Param("eventId") Long eventId);
}