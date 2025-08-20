package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String gameId; // 예: "game-01", "game-02"

    @Column(nullable = false)
    private String name; // 예: "Human Bingo", "Two Truths One Lie"

    @Column(length = 1000)
    private String description;

    @Lob
    private String instructions; // 게임 하는 방법 (상세)

    @Column(length = 500)
    private String exampleVideoUrl; // 예시 영상 링크

    @Column(nullable = false)
    private String category; // 예: "Icebreaker", "Team Building", "Social"

    @Column
    private Integer minParticipants; // 최소 참가자 수

    @Column
    private Integer maxParticipants; // 최대 참가자 수

    @Column
    private Integer estimatedDuration; // 예상 소요 시간 (분)

    @Column
    private String difficulty; // EASY, MEDIUM, HARD

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    // Constructors
    public Game() {}

    public Game(String gameId, String name, String description, String category) {
        this.gameId = gameId;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public Game(String gameId, String name, String description, String instructions, 
                String exampleVideoUrl, String category, Integer minParticipants, 
                Integer maxParticipants, Integer estimatedDuration, String difficulty) {
        this.gameId = gameId;
        this.name = name;
        this.description = description;
        this.instructions = instructions;
        this.exampleVideoUrl = exampleVideoUrl;
        this.category = category;
        this.minParticipants = minParticipants;
        this.maxParticipants = maxParticipants;
        this.estimatedDuration = estimatedDuration;
        this.difficulty = difficulty;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    // 새로운 필드들의 Getter/Setter
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getExampleVideoUrl() { return exampleVideoUrl; }
    public void setExampleVideoUrl(String exampleVideoUrl) { this.exampleVideoUrl = exampleVideoUrl; }

    public Integer getMinParticipants() { return minParticipants; }
    public void setMinParticipants(Integer minParticipants) { this.minParticipants = minParticipants; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public Integer getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(Integer estimatedDuration) { this.estimatedDuration = estimatedDuration; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
}
