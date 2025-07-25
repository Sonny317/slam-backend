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
    private String code;

    // ✅ 이 필드들이 누락되어 있었습니다.
    private String interests;
    private String spokenLanguages;
    private String desiredLanguages;

    // 약관 동의
    private boolean termsOfServiceAgreed;
    private boolean privacyPolicyAgreed;
    private boolean eventPhotoAgreed;
}