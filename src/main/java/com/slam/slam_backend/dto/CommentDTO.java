package com.slam.slam_backend.dto;

import com.slam.slam_backend.entity.Comment;
import java.time.LocalDateTime;

public class CommentDTO {
    private Long id;
    private String text;
    private String author;
    private String authorEmail;
    private String authorDisplayName;
    private LocalDateTime createdAt;

    // Constructors
    public CommentDTO() {}

    public CommentDTO(Comment comment) {
        this.id = comment.getId();
        this.text = comment.getText();
        this.author = comment.getAuthor();
        this.authorEmail = comment.getAuthor();
        this.authorDisplayName = null;
        this.createdAt = comment.getCreatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public void setAuthorDisplayName(String authorDisplayName) {
        this.authorDisplayName = authorDisplayName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
} 