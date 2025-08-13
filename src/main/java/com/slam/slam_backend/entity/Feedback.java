package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
@Getter
@Setter
@NoArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long eventId;

    // member or staff (for member form)
    @Column(nullable = false)
    private String role;

    // Ratings (1-5) and NPS (0-10)
    private Integer overall;               // 1-5
    private Integer participantsFit;       // 1-5
    private Integer interactionOpportunity;// 1-5
    private Integer languageConfidence;    // 1-5
    private Integer nps;                   // 0-10

    // Top 3 serialized as CSV for simplicity (e.g., "Traveling,Game Nights,Hiking")
    @Column(length = 255)
    private String top3Activities;

    @Lob
    private String comment;

    private LocalDateTime createdAt = LocalDateTime.now();
}


