// src/main/java/com/slam/slam_backend/dto/LoginResponse.java

package com.slam.slam_backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String token;
    private String email;
    private String name;
    private String profileImage;
    private String role; // ✅ 역할(role) 필드 추가
}