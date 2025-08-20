package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.GameAnalyticsDTO;
import com.slam.slam_backend.entity.Game;
import com.slam.slam_backend.entity.GameFeedback;
import com.slam.slam_backend.repository.GameRepository;
import com.slam.slam_backend.repository.GameFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 게임 분석을 위한 서비스
 * - 인원 수 대비 평점 정규화
 * - 게임별 통계 및 트렌드 분석
 * - 추천 시스템
 */
@Service
@RequiredArgsConstructor
public class GameAnalyticsService {

    private final GameRepository gameRepository;
    private final GameFeedbackRepository gameFeedbackRepository;

    /**
     * 특정 게임의 상세 분석 결과를 반환
     */
    public GameAnalyticsDTO analyzeGame(String gameId) {
        Game game = gameRepository.findByGameId(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다: " + gameId));

        List<GameFeedback> feedbacks = gameFeedbackRepository.findByGameIdOrderByCreatedAtDesc(gameId);

        if (feedbacks.isEmpty()) {
            return createEmptyAnalytics(game);
        }

        return GameAnalyticsDTO.builder()
                .gameId(gameId)
                .gameName(game.getName())
                .category(game.getCategory())
                .difficulty(game.getDifficulty())
                
                // 기본 통계
                .avgRating(calculateAverage(feedbacks, GameFeedback::getRating))
                .avgEngagement(calculateAverage(feedbacks, GameFeedback::getEngagement))
                .avgDifficulty(calculateAverage(feedbacks, GameFeedback::getDifficulty))
                .totalFeedbacks(feedbacks.size())
                .totalPlayCount(feedbacks.size()) // 현재는 피드백 수 = 플레이 횟수
                
                // 인원 수 분석
                .avgParticipants(calculateAverage(feedbacks, GameFeedback::getActualParticipants))
                .minParticipants(feedbacks.stream().mapToInt(GameFeedback::getActualParticipants).min().orElse(0))
                .maxParticipants(feedbacks.stream().mapToInt(GameFeedback::getActualParticipants).max().orElse(0))
                
                // 정규화된 점수
                .normalizedRating(calculateNormalizedRating(feedbacks))
                .suitableGroupSize(determineSuitableGroupSize(feedbacks))
                
                // 시간 분석
                .avgDuration(calculateAverageDuration(feedbacks))
                .recommendedDuration(game.getEstimatedDuration())
                
                // 최근 피드백
                .recentComments(getRecentComments(feedbacks, 5))
                
                // 트렌드 및 만족도
                .popularityTrend(calculatePopularityTrend(feedbacks))
                .participantSatisfaction(calculateSatisfactionScore(feedbacks))
                
                // 추천 점수
                .recommendationScore(calculateRecommendationScore(feedbacks, game))
                .recommendationReason(generateRecommendationReason(feedbacks, game))
                
                .build();
    }

    /**
     * 모든 게임의 요약 분석 결과를 반환
     */
    public List<GameAnalyticsDTO> analyzeAllGames() {
        List<Game> games = gameRepository.findByActiveTrue();
        return games.stream()
                .map(game -> analyzeGame(game.getGameId()))
                .sorted((a, b) -> Double.compare(b.getRecommendationScore(), a.getRecommendationScore()))
                .collect(Collectors.toList());
    }

    /**
     * 인원 수를 고려한 정규화된 평점 계산
     * 로직: 적정 인원 범위에서는 그대로, 너무 많거나 적으면 보정
     */
    private Double calculateNormalizedRating(List<GameFeedback> feedbacks) {
        if (feedbacks.isEmpty()) return 0.0;

        return feedbacks.stream()
                .mapToDouble(this::normalizeRatingByParticipants)
                .average()
                .orElse(0.0);
    }

    /**
     * 개별 피드백의 인원 수 대비 평점 정규화
     */
    private double normalizeRatingByParticipants(GameFeedback feedback) {
        int participants = feedback.getActualParticipants();
        double rating = feedback.getRating();
        
        // 적정 인원 범위 (5-15명)에서는 보정 없음
        if (participants >= 5 && participants <= 15) {
            return rating;
        }
        
        // 너무 적은 인원 (5명 미만): 약간 가산점
        if (participants < 5) {
            return Math.min(5.0, rating + 0.2);
        }
        
        // 너무 많은 인원 (15명 초과): 난이도에 따라 보정
        if (participants > 15) {
            // 대규모 게임에 적합한지 확인
            if (participants > 20) {
                return Math.max(1.0, rating - 0.3); // 20명 초과시 감점
            } else {
                return Math.max(1.0, rating - 0.1); // 15-20명은 약간 감점
            }
        }
        
        return rating;
    }

    /**
     * 적합한 그룹 사이즈 결정
     */
    private String determineSuitableGroupSize(List<GameFeedback> feedbacks) {
        // 높은 평점을 받은 인원 범위를 분석
        List<GameFeedback> highRatedFeedbacks = feedbacks.stream()
                .filter(f -> f.getRating() >= 4.0)
                .collect(Collectors.toList());
        
        if (highRatedFeedbacks.isEmpty()) {
            return "정보 부족";
        }
        
        double avgHighRatedParticipants = calculateAverage(highRatedFeedbacks, GameFeedback::getActualParticipants);
        
        if (avgHighRatedParticipants <= 8) {
            return "소규모 (3-8명)";
        } else if (avgHighRatedParticipants <= 15) {
            return "중규모 (9-15명)";
        } else {
            return "대규모 (16명+)";
        }
    }

    /**
     * 인기도 트렌드 계산 (최근 1개월 vs 이전 1개월)
     */
    private String calculatePopularityTrend(List<GameFeedback> feedbacks) {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        LocalDateTime twoMonthsAgo = LocalDateTime.now().minusMonths(2);
        
        long recentCount = feedbacks.stream()
                .filter(f -> f.getCreatedAt().isAfter(oneMonthAgo))
                .count();
        
        long previousCount = feedbacks.stream()
                .filter(f -> f.getCreatedAt().isAfter(twoMonthsAgo) && f.getCreatedAt().isBefore(oneMonthAgo))
                .count();
        
        if (previousCount == 0) return "신규";
        
        double changeRate = (double) (recentCount - previousCount) / previousCount;
        
        if (changeRate > 0.2) return "증가";
        if (changeRate < -0.2) return "감소";
        return "유지";
    }

    /**
     * 참가자 만족도 계산 (0-100점)
     */
    private Double calculateSatisfactionScore(List<GameFeedback> feedbacks) {
        if (feedbacks.isEmpty()) return 0.0;
        
        double avgRating = calculateAverage(feedbacks, GameFeedback::getRating);
        double avgEngagement = calculateAverage(feedbacks, GameFeedback::getEngagement);
        
        // 평점과 참여도를 종합하여 0-100점으로 변환
        return ((avgRating + avgEngagement) / 2.0 - 1.0) * 25.0; // 1-5 점수를 0-100으로 변환
    }

    /**
     * 종합 추천 점수 계산 (0-100점)
     */
    private Double calculateRecommendationScore(List<GameFeedback> feedbacks, Game game) {
        if (feedbacks.isEmpty()) return 0.0;
        
        double normalizedRating = calculateNormalizedRating(feedbacks);
        double consistency = calculateConsistency(feedbacks); // 평점의 일관성
        double frequency = Math.min(10.0, feedbacks.size()) / 10.0; // 최대 10회 기준으로 정규화
        
        // 가중 평균으로 추천 점수 계산
        double score = (normalizedRating * 0.4 + consistency * 0.3 + frequency * 0.3) - 1.0;
        return Math.max(0.0, Math.min(100.0, score * 25.0)); // 0-100점으로 변환
    }

    /**
     * 평점의 일관성 계산 (표준편차 역수)
     */
    private double calculateConsistency(List<GameFeedback> feedbacks) {
        if (feedbacks.size() < 2) return 5.0; // 데이터가 부족하면 중간값
        
        double avg = calculateAverage(feedbacks, GameFeedback::getRating);
        double variance = feedbacks.stream()
                .mapToDouble(f -> Math.pow(f.getRating() - avg, 2))
                .average()
                .orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        
        // 표준편차가 작을수록 일관성이 높음 (최대 5점)
        return Math.max(1.0, 5.0 - stdDev);
    }

    /**
     * 추천 이유 생성
     */
    private String generateRecommendationReason(List<GameFeedback> feedbacks, Game game) {
        double score = calculateRecommendationScore(feedbacks, game);
        double avgRating = calculateAverage(feedbacks, GameFeedback::getRating);
        String suitableSize = determineSuitableGroupSize(feedbacks);
        
        if (score >= 80) {
            return String.format("높은 평점(%.1f/5)과 안정적인 만족도를 보이는 추천 게임입니다. %s에 적합합니다.", avgRating, suitableSize);
        } else if (score >= 60) {
            return String.format("무난한 평점(%.1f/5)을 받고 있으며, %s 환경에서 활용 가능합니다.", avgRating, suitableSize);
        } else if (score >= 40) {
            return String.format("보통 수준의 만족도(%.1f/5)를 보입니다. 상황에 따라 선택적으로 사용하세요.", avgRating);
        } else {
            return String.format("낮은 평점(%.1f/5) 또는 데이터 부족으로 신중한 검토가 필요합니다.", avgRating);
        }
    }

    // Helper methods
    private GameAnalyticsDTO createEmptyAnalytics(Game game) {
        return GameAnalyticsDTO.builder()
                .gameId(game.getGameId())
                .gameName(game.getName())
                .category(game.getCategory())
                .difficulty(game.getDifficulty())
                .avgRating(0.0)
                .totalFeedbacks(0)
                .recommendationScore(0.0)
                .recommendationReason("아직 피드백이 없습니다.")
                .build();
    }

    private Double calculateAverage(List<GameFeedback> feedbacks, java.util.function.ToIntFunction<GameFeedback> mapper) {
        return feedbacks.stream()
                .mapToInt(mapper)
                .average()
                .orElse(0.0);
    }

    private Double calculateAverageDuration(List<GameFeedback> feedbacks) {
        return feedbacks.stream()
                .filter(f -> f.getActualDuration() != null)
                .mapToInt(GameFeedback::getActualDuration)
                .average()
                .orElse(0.0);
    }

    private List<String> getRecentComments(List<GameFeedback> feedbacks, int limit) {
        return feedbacks.stream()
                .filter(f -> f.getComment() != null && !f.getComment().trim().isEmpty())
                .limit(limit)
                .map(GameFeedback::getComment)
                .collect(Collectors.toList());
    }
}
