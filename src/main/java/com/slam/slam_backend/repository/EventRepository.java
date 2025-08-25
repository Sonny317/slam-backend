package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    // 지부(branch) 이름으로 이벤트를 찾는 메소드
    List<Event> findByBranch(String branch);
    
    // ✅ 지부별 최신 이벤트 조회
    Optional<Event> findTopByBranchOrderByEventDateTimeDesc(String branch);
    
    // ✅ 브랜치별 최대 event_sequence 조회
    @Query("SELECT MAX(e.eventSequence) FROM Event e WHERE e.branch = :branch AND e.eventSequence IS NOT NULL")
    Integer findMaxEventSequenceByBranch(@Param("branch") String branch);
}