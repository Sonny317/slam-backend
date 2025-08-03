package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonBackReference; // ✅ JSON 무한 루프 방지

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_memberships")
public class UserMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonBackReference // ✅ JSON 무한 루프 방지
    private User user;

    @Column(nullable = false)
    private String branchName; // 지부 이름 (e.g., "NCCU", "NTU")

    @Column(nullable = false)
    private String status; // 멤버십 상태 (e.g., "ACTIVE", "EXPIRED")

    // TODO: 추후에 멤버십 만료 날짜 등 필드 추가 가능
}