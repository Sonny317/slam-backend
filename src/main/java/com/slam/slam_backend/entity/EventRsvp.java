package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import lombok.*; // ✅ Setter를 위해 임포트 수정
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter // ✅ 이 어노테이션을 추가했습니다.
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "event_rsvps")
public class EventRsvp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private boolean isAttending;

    private boolean afterParty;

    @Column(nullable = false)
    private boolean attended; // QR 체크인 여부

    @CreationTimestamp
    private LocalDateTime createdAt;
}