package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.MembershipApplication;
import com.slam.slam_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipApplicationRepository extends JpaRepository<MembershipApplication, Long> {
    MembershipApplication findTopByUserOrderByCreatedAtDesc(User user);
    
    // ✅ 추가: 사용자의 승인 대기 중인 티켓 구매 신청 확인
    boolean existsByUserAndPaymentMethodAndStatus(User user, String paymentMethod, String status);
    
    // ✅ 추가: 특정 이벤트에 대한 사용자의 승인 대기 중인 티켓 구매 신청 확인
    boolean existsByUserAndPaymentMethodAndStatusAndEventId(User user, String paymentMethod, String status, Long eventId);
}