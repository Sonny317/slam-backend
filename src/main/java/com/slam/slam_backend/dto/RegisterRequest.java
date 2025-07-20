// src/main/java/com/slam/slam_backend/dto/RegisterRequest.java

package com.slam.slam_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    // 기본 정보
    private String name;
    private String email;
    private String password;

    private String code; // ✅ 이메일 인증코드 필드 다시 추가



    // 약관 동의
    private boolean termsOfServiceAgreed;
    private boolean privacyPolicyAgreed;
    private boolean eventPhotoAgreed;
}