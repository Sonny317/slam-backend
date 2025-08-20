package com.slam.slam_backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 사용자 기본 정보 업데이트를 위한 DTO
 */
@Data
public class UserUpdateRequest {

    /**
     * 이름
     */
    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    /**
     * 전화번호 (선택사항)
     */
    private String phone;

    /**
     * 전공 (선택사항)
     */
    private String major;

    /**
     * 학번 (선택사항)
     */
    private String studentId;

    /**
     * 자기소개 (선택사항)
     */
    private String bio;

    /**
     * 관심사 (선택사항)
     */
    private String interests;

    /**
     * 구사 언어 (선택사항)
     */
    private String spokenLanguages;

    /**
     * 배우고 싶은 언어 (선택사항)
     */
    private String desiredLanguages;
}
