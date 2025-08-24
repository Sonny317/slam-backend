package com.slam.slam_backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "polls")
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "post_id", nullable = false)
    @JsonBackReference
    private Post post;

    @Column(nullable = false)
    private String question;

    @Column(name = "allow_multi", nullable = false)
    private Boolean allowMulti = false;

    @Column(name = "closes_at")
    private LocalDateTime closesAt;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    @JsonManagedReference
    private List<PollOption> options = new ArrayList<>();

    public Poll() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public Boolean getAllowMulti() { return allowMulti; }
    public void setAllowMulti(Boolean allowMulti) { this.allowMulti = allowMulti; }

    public LocalDateTime getClosesAt() { return closesAt; }
    public void setClosesAt(LocalDateTime closesAt) { this.closesAt = closesAt; }

    public List<PollOption> getOptions() { return options; }
    public void setOptions(List<PollOption> options) { this.options = options; }
}


