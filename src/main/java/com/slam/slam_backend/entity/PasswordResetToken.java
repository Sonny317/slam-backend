package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    private static final int EXPIRATION_MINUTES = 30; // 토큰 유효 시간: 30분

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public PasswordResetToken(User user, String token) {
        this.user = user;
        this.token = token;
        this.expiryDate = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}