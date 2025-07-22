package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    // 지부(branch) 이름으로 이벤트를 찾는 메소드
    List<Event> findByBranch(String branch);
}