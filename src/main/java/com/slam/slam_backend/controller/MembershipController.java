package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.MembershipRequest;
import com.slam.slam_backend.entity.MembershipApplication;
import com.slam.slam_backend.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @PostMapping("/apply")
    public ResponseEntity<?> applyForMembership(@RequestBody MembershipRequest request, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("인증이 필요합니다.");
        }

        try {
            String userEmail = authentication.getName();
            MembershipApplication application = membershipService.applyForMembership(userEmail, request);

            // 친구가 요청한 응답 형식
            Map<String, Object> response = Map.of(
                    "applicationId", application.getId(),
                    "status", application.getStatus()
            );
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("신청 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}