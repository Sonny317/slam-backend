package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "game_feedbacks")
@Getter
@Setter
@NoArgsConstructor
public class GameFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private String gameId;

    @Column(nullable = false)
    private Integer rating; // 1-5
}


