package com.slam.slam_backend.dto;

import lombok.Getter;
import lombok.Setter;

    @Getter
    @Setter
    public class RegisterRequest {
        private String email;
        private String password;
        private String code; // ✅ 인증 코드
        private String role;
    }


