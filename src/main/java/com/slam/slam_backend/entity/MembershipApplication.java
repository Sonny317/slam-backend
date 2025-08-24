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

    private String userType; // Local, International, Exchange 등
    private String studentId;
    private String major;
    private String otherMajor;
    private String professionalStatus;
    private String country;
    private String phone;
    private String foodAllergies;
    private String paymentMethod;
    private String bankLast5;

    @Column(nullable = false)
    private String status; // 신청 상태 (예: "payment_pending")

    private Long eventId; // 티켓 구매인 경우 해당 이벤트 ID

    @CreationTimestamp
    private LocalDateTime createdAt; // 신청일

    // 명시적으로 getter 메서드들 추가 (Lombok 백업용)
    public String getMajor() {
        return major;
    }

    public String getOtherMajor() {
        return otherMajor;
    }

    public String getProfessionalStatus() {
        return professionalStatus;
    }

    public String getCountry() {
        return country;
    }

    public String getPhone() {
        return phone;
    }

    public String getFoodAllergies() {
        return foodAllergies;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getBankLast5() {
        return bankLast5;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}