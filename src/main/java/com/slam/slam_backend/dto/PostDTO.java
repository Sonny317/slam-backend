package com.slam.slam_backend.dto;

import com.slam.slam_backend.entity.Post;
import com.slam.slam_backend.entity.Poll;
import com.slam.slam_backend.entity.PollOption;
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
    private String authorEmail; // stable identifier (email)
    private String authorDisplayName; // display-only name
    private LocalDateTime createdAt;
    private Integer views;
    private Integer likes;
    private Boolean isPinned;
    private List<CommentDTO> comments;

    // Poll fields
    private String pollQuestion;
    private Boolean pollAllowMulti;
    private List<String> pollOptions;
    private int[] pollVotes;
    private Boolean hasUserVoted;
    private Integer userVoteIndex;

    // Constructors
    public PostDTO() {}

    public PostDTO(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.summary = post.getSummary();
        this.category = post.getCategory();
        this.author = post.getAuthor();
        this.authorEmail = post.getAuthor();
        try {
            // try resolving latest display name from UserRepository via a static accessor avoided; keep null here
            this.authorDisplayName = null;
        } catch (Exception ignored) {}
        this.createdAt = post.getCreatedAt();
        this.views = post.getViews();
        this.likes = post.getLikes();
        this.isPinned = post.getIsPinned();
        this.comments = post.getComments().stream()
                .map(CommentDTO::new)
                .collect(Collectors.toList());

        try {
            // Reflective access avoided; poll is optional: if Post has a mapped poll relation later
            java.lang.reflect.Field pollField = Post.class.getDeclaredField("poll");
            pollField.setAccessible(true);
            Object pollObj = pollField.get(post);
            if (pollObj instanceof Poll) {
                Poll poll = (Poll) pollObj;
                this.pollQuestion = poll.getQuestion();
                this.pollAllowMulti = poll.getAllowMulti();
                if (poll.getOptions() != null) {
                    this.pollOptions = poll.getOptions().stream()
                            .map(PollOption::getOptionText)
                            .collect(Collectors.toList());
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {}
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

    public String getPollQuestion() { return pollQuestion; }
    public void setPollQuestion(String pollQuestion) { this.pollQuestion = pollQuestion; }
    public Boolean getPollAllowMulti() { return pollAllowMulti; }
    public void setPollAllowMulti(Boolean pollAllowMulti) { this.pollAllowMulti = pollAllowMulti; }
    public List<String> getPollOptions() { return pollOptions; }
    public void setPollOptions(List<String> pollOptions) { this.pollOptions = pollOptions; }
    public int[] getPollVotes() { return pollVotes; }
    public void setPollVotes(int[] pollVotes) { this.pollVotes = pollVotes; }
    public Boolean getHasUserVoted() { return hasUserVoted; }
    public void setHasUserVoted(Boolean hasUserVoted) { this.hasUserVoted = hasUserVoted; }
    public Integer getUserVoteIndex() { return userVoteIndex; }
    public void setUserVoteIndex(Integer userVoteIndex) { this.userVoteIndex = userVoteIndex; }
} 