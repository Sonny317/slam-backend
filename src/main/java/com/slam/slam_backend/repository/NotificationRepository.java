package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    
    List<Notification> findByUserIdAndReadOrderByCreatedAtDesc(String userId, Boolean read);
    
    long countByUserIdAndRead(String userId, Boolean read);
    
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId")
    void markAllAsReadByUserId(@Param("userId") String userId);
    
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :id AND n.userId = :userId")
    void markAsReadByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);
}
