package com.slam.slam_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "event_games")
public class EventGame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private String gameId; // Game 테이블의 gameId 참조

    // Constructors
    public EventGame() {}

    public EventGame(Long eventId, String gameId) {
        this.eventId = eventId;
        this.gameId = gameId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
}
