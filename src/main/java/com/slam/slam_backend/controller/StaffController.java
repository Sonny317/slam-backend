package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.StaffAssignmentRequest;
import com.slam.slam_backend.dto.StaffOnboardingRequest;
import com.slam.slam_backend.entity.StaffOnboardingToken;
import com.slam.slam_backend.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    /**
     * 스태프 임명 시작 (PRESIDENT/ADMIN 전용)
     */
    @PostMapping("/assign")
    public ResponseEntity<?> assignStaff(
            @Valid @RequestBody StaffAssignmentRequest request,
            Authentication authentication) {
        try {
            String assignerEmail = authentication.getName();
            staffService.initiateStaffAssignment(assignerEmail, request);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "스태프 임명 이메일이 발송되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 온보딩 토큰 유효성 확인 및 정보 조회
     */
    @GetMapping("/onboarding/{token}")
    public ResponseEntity<?> getOnboardingInfo(@PathVariable String token) {
        try {
            StaffOnboardingToken onboarding = staffService.getOnboardingByToken(token);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "userName", onboarding.getUser().getName(),
                            "userEmail", onboarding.getUser().getEmail(),
                            "targetRole", onboarding.getTargetRole(),
                            "assignedBy", onboarding.getAssignedBy().getName(),
                            "expiryDate", onboarding.getExpiryDate()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 스태프 온보딩 완료 - 상세 정보 입력
     */
    @PostMapping("/onboarding/complete")
    public ResponseEntity<?> completeOnboarding(@Valid @RequestBody StaffOnboardingRequest request) {
        try {
            staffService.completeStaffOnboarding(request);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "스태프 온보딩이 완료되었습니다. 환영합니다!"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 진행 중인 온보딩 목록 조회 (관리자용)
     */
    @GetMapping("/onboarding/pending")
    public ResponseEntity<?> getPendingOnboardings(Authentication authentication) {
        try {
            // 권한 확인은 보안 설정에서 처리하거나 서비스에서 확인
            List<StaffOnboardingToken> pending = staffService.getPendingOnboardings();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", pending
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 만료된 토큰 정리 (관리자용)
     */
    @DeleteMapping("/onboarding/cleanup")
    public ResponseEntity<?> cleanupExpiredTokens(Authentication authentication) {
        try {
            staffService.cleanupExpiredTokens();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "만료된 토큰이 정리되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
