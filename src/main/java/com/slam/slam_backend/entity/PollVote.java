package com.slam.slam_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "poll_votes", uniqueConstraints = {
        @UniqueConstraint(name = "uq_poll_user", columnNames = {"poll_id", "user_id"})
})
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne
    @JoinColumn(name = "option_id", nullable = false)
    private PollOption option;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public PollVote() {}

    public PollVote(Poll poll, PollOption option, User user) {
        this.poll = poll;
        this.option = option;
        this.user = user;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Poll getPoll() { return poll; }
    public void setPoll(Poll poll) { this.poll = poll; }

    public PollOption getOption() { return option; }
    public void setOption(PollOption option) { this.option = option; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}



