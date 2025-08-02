package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.EventDTO;
import com.slam.slam_backend.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.slam.slam_backend.dto.ApplicationDTO; // ✅ DTO 임포트
import com.slam.slam_backend.dto.EventRequest; // ✅ 임포트 추가
import com.slam.slam_backend.entity.Event; // ✅ 임포트 추가
import com.slam.slam_backend.service.EventService; // ✅ 임포트 추가

import java.util.Map; // ✅ Map 임포트 추가

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MembershipService membershipService;
    private final EventService eventService; // ✅ EventService 주입
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

    // --- ✅ 이벤트 관리 API 추가 ---

    @PostMapping("/events")
    public ResponseEntity<EventDTO> createEvent(@RequestBody EventRequest request) {
        Event newEvent = eventService.createEvent(request);
        return ResponseEntity.ok(EventDTO.fromEntity(newEvent));
    }

    @PutMapping("/events/{eventId}")
    public ResponseEntity<EventDTO> updateEvent(@PathVariable Long eventId, @RequestBody EventRequest request) {
        try {
            Event updatedEvent = eventService.updateEvent(eventId, request);
            return ResponseEntity.ok(EventDTO.fromEntity(updatedEvent));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long eventId) {
        try {
            eventService.deleteEvent(eventId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Event deleted successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}