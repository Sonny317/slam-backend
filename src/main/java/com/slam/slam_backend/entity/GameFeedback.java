package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private String gameId;

    @Column(nullable = false)
    private Integer rating; // 1-5 (전체적인 재미)

    @Column(nullable = false)
    private Integer engagement; // 1-5 (참여도)

    @Column(nullable = false)
    private Integer difficulty; // 1-5 (난이도 적절성)

    @Column(columnDefinition = "TEXT")
    private String comment; // 텍스트 피드백

    @Column(nullable = false)
    private Integer actualParticipants; // 실제 참가자 수

    @Column
    private Integer actualDuration; // 실제 소요 시간 (분)

    @Column(length = 100)
    private String submittedBy; // 피드백 작성자 (익명화된 ID)

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 인원 수 대비 점수 계산을 위한 메모 필드
    @Column(columnDefinition = "TEXT")
    private String organizerNotes; // 주최자 메모 (예: "인원이 많아서 시간이 오래 걸렸음", "소규모라 재미있었음")
}


