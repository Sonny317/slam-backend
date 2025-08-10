package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findByEmail(String email);

    // ✅ MySQL upsert: 중복 키(이메일)일 경우 code/expiry_date 업데이트
    @Modifying
    @Query(value = "INSERT INTO verification_codes (email, code, expiry_date) VALUES (:email, :code, :expiry) " +
            "ON DUPLICATE KEY UPDATE code = VALUES(code), expiry_date = VALUES(expiry_date)", nativeQuery = true)
    void upsertCode(@Param("email") String email,
                    @Param("code") String code,
                    @Param("expiry") LocalDateTime expiry);
}