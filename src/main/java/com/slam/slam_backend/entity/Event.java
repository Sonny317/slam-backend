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

    private int capacity; // 총 정원

    private int currentAttendees; // 현재 참석 인원

    private int price; // 가격

    // 이벤트가 과거 처리(보관)되었는지 여부
    private boolean archived;
}