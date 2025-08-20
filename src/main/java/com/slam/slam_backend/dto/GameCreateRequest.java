package com.slam.slam_backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * 게임 생성/수정 요청 DTO
 */
@Data
public class GameCreateRequest {

    @NotBlank(message = "게임 이름을 입력해주세요.")
    private String name;

    @NotBlank(message = "게임 설명을 입력해주세요.")
    private String description;

    private String instructions; // 게임 하는 방법

    private String exampleVideoUrl; // 예시 영상 링크

    @NotBlank(message = "카테고리를 선택해주세요.")
    private String category; // Icebreaker, Team Building, Social, etc.

    @Min(value = 1, message = "최소 참가자 수는 1명 이상이어야 합니다.")
    private Integer minParticipants;

    @Max(value = 100, message = "최대 참가자 수는 100명 이하여야 합니다.")
    private Integer maxParticipants;

    @Min(value = 1, message = "예상 소요 시간은 1분 이상이어야 합니다.")
    private Integer estimatedDuration; // 분 단위

    private String difficulty; // EASY, MEDIUM, HARD

    private Boolean active = true; // 활성화 여부
}
