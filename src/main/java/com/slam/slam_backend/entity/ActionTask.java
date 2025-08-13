package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "action_tasks")
@Getter
@Setter
@NoArgsConstructor
public class ActionTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String team; // GA, PR, EP

    private LocalDate deadline;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String agenda; // HTML content

    @Column(nullable = false)
    private String status; // todo, inProgress, done

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Branch: NCCU / NTU / TAIPEI
    @Column(nullable = false)
    private String branch = "NCCU";

    // Optional: which event this task relates to (store title for simplicity)
    private String eventTitle;

    // Archived flag for moving out of active boards
    @Column(nullable = false)
    private boolean archived = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "action_task_acks",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> acknowledgedBy = new HashSet<>();
}


