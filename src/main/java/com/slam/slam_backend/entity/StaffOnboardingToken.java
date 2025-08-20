package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 스태프 온보딩 프로세스를 관리하는 엔티티
 * 스태프 임명 시 생성되며, 상세 정보 입력 완료 시까지 유지됨
 */
@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "staff_onboarding_tokens")
public class StaffOnboardingToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 스태프로 임명될 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 스태프를 임명한 사용자 (PRESIDENT 또는 ADMIN)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy;

    /**
     * 임명될 스태프의 역할
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole targetRole;

    /**
     * 고유 토큰 (이메일 링크에 사용)
     */
    @Column(nullable = false, unique = true, length = 255)
    private String token;

    /**
     * 토큰 만료일시 (기본 7일)
     */
    @Column(nullable = false)
    private LocalDateTime expiryDate;

    /**
     * 온보딩 완료 여부
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean completed = false;

    /**
     * 토큰 생성일시
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 온보딩 완료일시
     */
    private LocalDateTime completedAt;

    /**
     * 토큰이 만료되었는지 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * 온보딩을 완료로 표시
     */
    public void markAsCompleted() {
        this.completed = true;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 기본 생성자에서 만료일시 설정 (7일 후)
     */
    @PrePersist
    public void prePersist() {
        if (expiryDate == null) {
            expiryDate = LocalDateTime.now().plusDays(7);
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
