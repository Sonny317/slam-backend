package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String branch; // 지부 (e.g., "NCCU", "TAIPEI")

    @Column(nullable = false)
    private String title; // 이벤트 제목

    private String theme; // 이벤트 테마 (e.g., "Language Exchange")

    @Column(nullable = false)
    private LocalDateTime eventDateTime; // 이벤트 날짜 및 시간

    @Column(nullable = false)
    private String location; // 장소

    @Column(columnDefinition = "TEXT")
    private String description; // 상세 설명

    private String imageUrl; // 대표 이미지 URL

    @Column(nullable = false)
    private int capacity; // 총 정원

    @Column(nullable = false)
    private int currentAttendees; // 현재 참석 인원

    @Column(nullable = false)
    private int price; // 일반 가격

    // ✅ 얼리버드 가격 관련 필드
    private Integer earlyBirdPrice; // 얼리버드 가격 (nullable)
    private LocalDateTime earlyBirdEndDate; // 얼리버드 기간 종료일
    private Integer earlyBirdCapacity; // 얼리버드 인원수 제한

    // ✅ 등록 데드라인
    private LocalDateTime registrationDeadline; // 등록 마감일

    // ✅ 용량 경고 표시 옵션
    private Integer capacityWarningThreshold; // 남은 자리 경고 표시 임계값 (예: 20명 남으면 표시)
    private Boolean showCapacityWarning; // 용량 경고 표시 여부

    // ✅ 종료 시간 추가
    private String endTime; // 종료 시간

    // ✅ 계좌 정보
    private String bankAccount; // 계좌 번호

    // 이벤트가 과거 처리(보관)되었는지 여부
    @Column(nullable = false)
    private boolean archived;
}