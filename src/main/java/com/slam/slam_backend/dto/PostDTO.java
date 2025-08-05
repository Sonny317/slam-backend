package com.slam.slam_backend.dto;

import com.slam.slam_backend.entity.Post;
import com.slam.slam_backend.entity.Comment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private String summary;
    private String category;
    private String author;
    private LocalDateTime createdAt;
    private Integer views;
    private Integer likes;
    private Boolean isPinned;
    private List<CommentDTO> comments;

    // Constructors
    public PostDTO() {}

    public PostDTO(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.summary = post.getSummary();
        this.category = post.getCategory();
        this.author = post.getAuthor();
        this.createdAt = post.getCreatedAt();
        this.views = post.getViews();
        this.likes = post.getLikes();
        this.isPinned = post.getIsPinned();
        this.comments = post.getComments().stream()
                .map(CommentDTO::new)
                .collect(Collectors.toList());
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public Boolean getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(Boolean isPinned) {
        this.isPinned = isPinned;
    }

    public List<CommentDTO> getComments() {
        return comments;
    }

    public void setComments(List<CommentDTO> comments) {
        this.comments = comments;
    }
} 