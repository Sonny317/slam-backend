package com.slam.slam_backend.controller;

import com.slam.slam_backend.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MembershipService membershipService;

    // 멤버십 신청자 전체 목록을 조회하는 API
    @GetMapping("/membership-applications")
    public ResponseEntity<List<?>> getAllMembershipApplications() {
        // TODO: 응답 형식을 DTO로 변환하는 것이 좋습니다.
        return ResponseEntity.ok(membershipService.findAllApplications());
    }
}