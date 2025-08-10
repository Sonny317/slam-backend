package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Modifying;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findByEmail(String email);

    // 벤더 의존적인 upsert 쿼리는 제거하고, 서비스 레이어에서 삭제 후 저장 전략을 사용합니다.
}