package com.slam.slam_backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * 게임 피드백 생성 요청 DTO
 */
@Data
public class GameFeedbackCreateRequest {

    @NotNull(message = "이벤트 ID를 입력해주세요.")
    private Long eventId;

    @NotNull(message = "게임 ID를 입력해주세요.")
    private String gameId;

    @NotNull(message = "전체 평점을 입력해주세요.")
    @Min(value = 1, message = "평점은 1 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5 이하여야 합니다.")
    private Integer rating; // 1-5

    @NotNull(message = "참여도 점수를 입력해주세요.")
    @Min(value = 1, message = "참여도는 1 이상이어야 합니다.")
    @Max(value = 5, message = "참여도는 5 이하여야 합니다.")
    private Integer engagement; // 1-5

    @NotNull(message = "난이도 점수를 입력해주세요.")
    @Min(value = 1, message = "난이도는 1 이상이어야 합니다.")
    @Max(value = 5, message = "난이도는 5 이하여야 합니다.")
    private Integer difficulty; // 1-5

    private String comment; // 텍스트 피드백

    @NotNull(message = "실제 참가자 수를 입력해주세요.")
    @Min(value = 1, message = "참가자 수는 1명 이상이어야 합니다.")
    private Integer actualParticipants;

    @Min(value = 1, message = "소요 시간은 1분 이상이어야 합니다.")
    private Integer actualDuration; // 분 단위

    private String organizerNotes; // 주최자 메모
}
