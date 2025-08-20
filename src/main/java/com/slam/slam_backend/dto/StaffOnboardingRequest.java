package com.slam.slam_backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

/**
 * 스태프 온보딩 시 상세 정보 입력을 위한 DTO
 */
@Data
public class StaffOnboardingRequest {

    /**
     * 온보딩 토큰
     */
    @NotBlank(message = "토큰이 필요합니다.")
    private String token;

    /**
     * 국적
     */
    @NotBlank(message = "국적을 입력해주세요.")
    private String nationality;

    /**
     * 학번 (선택사항)
     */
    private String studentId;

    /**
     * 전공
     */
    @NotBlank(message = "전공을 입력해주세요.")
    private String major;

    /**
     * 전화번호
     */
    @NotBlank(message = "전화번호를 입력해주세요.")
    private String phoneNumber;

    /**
     * 소속 학교
     */
    @NotBlank(message = "소속 학교를 입력해주세요.")
    private String university;

    /**
     * 소속 팀 (GA, PR, EP 등)
     */
    @NotBlank(message = "소속 팀을 선택해주세요.")
    private String team;

    /**
     * 자기소개 (선택사항)
     */
    private String bio;

    /**
     * 소속/직책 정보 업데이트
     */
    private String affiliation;
}
