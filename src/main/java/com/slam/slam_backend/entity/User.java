package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonManagedReference; // ✅ JSON 무한 루프 방지

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set; // ✅ 임포트 추가

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User implements UserDetails { // ✅ UserDetails 구현

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 명시적으로 getId() 메서드 추가 (Lombok 백업용)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 50)
    private String affiliation;

    @Column(length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Column
    private String interests;

    @Column
    private String membership;

    @Column(name = "profile_image_url")
    private String profileImage;

    @Column
    private String spokenLanguages;

    @Column
    private String desiredLanguages;

    @Column(length = 50)
    private String studentId;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String major;

    @Column(length = 50)
    private String nationality;

    // ✅ 가입 날짜 추가
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ✅ 사용자가 가진 모든 멤버십 정보를 담을 '보관함'과의 연결을 추가합니다.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference // ✅ JSON 무한 루프 방지
    private Set<UserMembership> memberships;

    // --- UserDetails 인터페이스 메소드 구현 ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * 엔티티 저장 전 자동으로 가입 날짜 설정
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}