package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.StaffOnboardingToken;
import com.slam.slam_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffOnboardingTokenRepository extends JpaRepository<StaffOnboardingToken, Long> {

    /**
     * 토큰으로 온보딩 정보 조회
     */
    Optional<StaffOnboardingToken> findByToken(String token);

    /**
     * 사용자별 미완료 온보딩 토큰 조회
     */
    Optional<StaffOnboardingToken> findByUserAndCompleted(User user, boolean completed);

    /**
     * 특정 사용자의 모든 온보딩 토큰 조회
     */
    List<StaffOnboardingToken> findByUser(User user);

    /**
     * 만료된 토큰들 조회
     */
    @Query("SELECT s FROM StaffOnboardingToken s WHERE s.expiryDate < :now AND s.completed = false")
    List<StaffOnboardingToken> findExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 특정 임명자가 생성한 토큰들 조회
     */
    List<StaffOnboardingToken> findByAssignedBy(User assignedBy);

    /**
     * 미완료 토큰들 조회
     */
    List<StaffOnboardingToken> findByCompleted(boolean completed);

    /**
     * 토큰이 유효한지 확인 (만료되지 않고 미완료인 토큰)
     */
    @Query("SELECT s FROM StaffOnboardingToken s WHERE s.token = :token AND s.completed = false AND s.expiryDate > :now")
    Optional<StaffOnboardingToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);
}
