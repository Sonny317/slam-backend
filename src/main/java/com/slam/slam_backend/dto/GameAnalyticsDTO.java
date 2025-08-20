package com.slam.slam_backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 게임 분석 결과를 담는 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameAnalyticsDTO {

    private String gameId;
    private String gameName;
    private String category;
    private String difficulty;

    // 기본 통계
    private Double avgRating;           // 전체 평균 평점
    private Double avgEngagement;       // 평균 참여도
    private Double avgDifficulty;       // 평균 난이도 점수
    private Integer totalFeedbacks;     // 총 피드백 수
    private Integer totalPlayCount;     // 총 플레이 횟수

    // 인원 수 분석
    private Double avgParticipants;     // 평균 참가자 수
    private Integer minParticipants;    // 최소 참가자 수
    private Integer maxParticipants;    // 최대 참가자 수

    // 정규화된 점수 (인원 수 고려)
    private Double normalizedRating;    // 인원 수 대비 정규화된 평점
    private String suitableGroupSize;   // 적합한 인원 규모 ("소규모", "중규모", "대규모")

    // 시간 분석
    private Double avgDuration;         // 평균 소요 시간
    private Integer recommendedDuration; // 권장 소요 시간

    // 최근 피드백
    private List<String> recentComments; // 최근 코멘트들 (최대 5개)

    // 트렌드 분석
    private String popularityTrend;     // "증가", "유지", "감소"
    private Double participantSatisfaction; // 참가자 만족도 (0-100)

    // 추천 점수 (종합)
    private Double recommendationScore; // 0-100점 (모든 요소를 고려한 추천 점수)
    private String recommendationReason; // 추천/비추천 이유
}
