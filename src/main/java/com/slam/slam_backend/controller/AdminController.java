package com.slam.slam_backend.controller;

import com.slam.slam_backend.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.slam.slam_backend.dto.ApplicationDTO; // ✅ DTO 임포트

import java.util.Map; // ✅ Map 임포트 추가

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MembershipService membershipService;

    // ✅ 반환 타입을 ResponseEntity<List<ApplicationDTO>>로 수정
    @GetMapping("/membership-applications")
    public ResponseEntity<List<ApplicationDTO>> getAllMembershipApplications() {
        return ResponseEntity.ok(membershipService.findAllApplications());
    }

    // ✅ 추가: 멤버십 신청을 승인하는 API
    @PostMapping("/applications/{applicationId}/approve")
    public ResponseEntity<?> approveApplication(@PathVariable Long applicationId) {
        try {
            membershipService.approveApplication(applicationId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Application approved successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}