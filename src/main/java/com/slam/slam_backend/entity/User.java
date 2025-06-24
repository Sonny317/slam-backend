// src/main/java/com/slam/slam_backend/entity/User.java

package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name; // ✅ 이름 추가

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 50)
    private String affiliation; // ✅ 소속 추가

    @Column // ✅ 관심사 (쉼표로 구분된 문자열)
    private String interests;

    @Column // ✅ 구사 언어
    private String spokenLanguages;

    @Column // ✅ 배우고 싶은 언어
    private String desiredLanguages;

    @Column(nullable = false, length = 20)
    private String role; // 예: USER, ADMIN
    
    @Column(length = 500) // ✅ 자기소개 필드
    private String bio;
    
    @Column(name = "profile_image_url")
    private String profileImage;
}