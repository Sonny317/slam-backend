package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "membership_applications")
public class MembershipApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 신청서 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 신청한 사용자

    @Column(nullable = false)
    private String selectedBranch; // 선택한 지부 (NCCU, NTU 등)

    @Column(nullable = false)
    private String status; // 신청 상태 (예: "payment_pending")

    @Column(nullable = false)
    private String paymentMethod; // 결제 방법

    private Long eventId; // 티켓 구매인 경우 해당 이벤트 ID

    @CreationTimestamp
    private LocalDateTime createdAt; // 신청일

    // 명시적으로 getter 메서드들 추가 (Lombok 백업용)
    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }
}