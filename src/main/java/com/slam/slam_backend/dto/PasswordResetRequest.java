package com.slam.slam_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequest {
    private String token;
    private String newPassword;
}