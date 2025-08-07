package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.ApplicationDTO;
import com.slam.slam_backend.dto.EventDTO;
import com.slam.slam_backend.entity.Event;
import com.slam.slam_backend.entity.MembershipApplication;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.UserMembership;
import com.slam.slam_backend.repository.EventRepository;
import com.slam.slam_backend.repository.MembershipApplicationRepository;
import com.slam.slam_backend.repository.UserMembershipRepository;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.service.EventService;
import com.slam.slam_backend.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MembershipService membershipService;
    private final EventService eventService;
    private final MembershipApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final EventRepository eventRepository;

    // ✅ 이벤트 관리 API들
    @GetMapping("/events")
    public ResponseEntity<List<EventDTO>> getAllEvents() {
        List<Event> events = eventRepository.findAll();
        List<EventDTO> eventDTOs = events.stream()
                .map(EventDTO::fromEntity)
                .collect(Collectors.toList());
        System.out.println("AdminPage API called - Total events: " + eventDTOs.size());
        System.out.println("Events: " + eventDTOs.stream().map(e -> e.getTitle() + " (" + e.getBranch() + ")").collect(Collectors.toList()));
        return ResponseEntity.ok(eventDTOs);
    }

    @PostMapping("/events")
    public ResponseEntity<EventDTO> createEvent(@RequestBody EventDTO eventDTO) {
        try {
            Event event = eventDTO.toEntity();
            Event savedEvent = eventRepository.save(event);
            return ResponseEntity.ok(EventDTO.fromEntity(savedEvent));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/events")
    public ResponseEntity<EventDTO> updateEvent(@RequestParam Long eventId, @RequestBody EventDTO eventDTO) {
        try {
            Event existingEvent = eventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            
            // 기존 이벤트 정보 업데이트
            existingEvent.setTitle(eventDTO.getTitle());
            existingEvent.setDescription(eventDTO.getDescription());
            existingEvent.setLocation(eventDTO.getLocation());
            existingEvent.setEventDateTime(eventDTO.getEventDateTime());
            existingEvent.setBranch(eventDTO.getBranch());
            existingEvent.setCapacity(eventDTO.getCapacity());
            
            Event savedEvent = eventRepository.save(existingEvent);
            return ResponseEntity.ok(EventDTO.fromEntity(savedEvent));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/events")
    public ResponseEntity<?> deleteEvent(@RequestParam Long eventId) {
        try {
            eventRepository.deleteById(eventId);
            return ResponseEntity.ok("Event deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/membership-applications")
    public ResponseEntity<List<ApplicationDTO>> getMembershipApplications() {
        List<MembershipApplication> applications = applicationRepository.findAll();
        List<ApplicationDTO> applicationDTOs = applications.stream()
                .map(ApplicationDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(applicationDTOs);
    }

    @PostMapping("/applications/approve")
    public ResponseEntity<?> approveApplication(@RequestParam Long applicationId) {
        try {
            membershipService.approveApplication(applicationId);
            return ResponseEntity.ok("Application approved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/applications/reject")
    public ResponseEntity<?> rejectApplication(@RequestParam Long applicationId) {
        try {
            membershipService.rejectApplication(applicationId);
            return ResponseEntity.ok("Application rejected successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ 멤버십 삭제 API 추가
    @DeleteMapping("/users/memberships")
    public ResponseEntity<?> deleteUserMembership(@RequestParam Long userId, @RequestParam String branchName) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // UserMembership 테이블에서 해당 멤버십 삭제 (userId 사용)
            List<UserMembership> memberships = userMembershipRepository.findByUserIdAndBranchName(userId, branchName);
            if (!memberships.isEmpty()) {
                userMembershipRepository.deleteAll(memberships);
            }
            
            // User 엔티티의 membership 필드도 업데이트 (단일 멤버십인 경우)
            if (branchName.equals(user.getMembership()) || 
                (user.getMembership() != null && user.getMembership().contains(branchName))) {
                user.setMembership(null);
                userRepository.save(user);
            }
            
            return ResponseEntity.ok("Membership deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ 모든 사용자 목록 조회 API
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    // ✅ 특정 지부의 멤버 목록 조회 API
    @GetMapping("/users/branch")
    public ResponseEntity<List<User>> getUsersByBranch(@RequestParam String branchName) {
        // 정확한 멤버십 매칭으로 변경
        List<User> users = userRepository.findByExactMembership(branchName);
        return ResponseEntity.ok(users);
    }

    // ✅ 모든 사용자의 멤버십 초기화 API (테스트용)
    @PostMapping("/reset-memberships")
    public ResponseEntity<?> resetAllMemberships() {
        try {
            List<User> users = userRepository.findAll();
            int resetCount = 0;
            
            // 디버그 로그 추가
            System.out.println("=== Reset Memberships Debug ===");
            System.out.println("Total users found: " + users.size());
            
            for (User user : users) {
                System.out.println("User ID: " + user.getId() + ", Name: " + user.getName() + ", Membership: " + user.getMembership());
                // 모든 사용자의 membership 필드를 null로 설정
                user.setMembership(null);
                userRepository.save(user);
                resetCount++;
            }
            
            // UserMembership 테이블 상태 확인
            long membershipCount = userMembershipRepository.count();
            System.out.println("UserMembership records before deletion: " + membershipCount);
            
            // UserMembership 테이블도 초기화
            userMembershipRepository.deleteAll();
            
            long membershipCountAfter = userMembershipRepository.count();
            System.out.println("UserMembership records after deletion: " + membershipCountAfter);
            
            return ResponseEntity.ok("Reset " + resetCount + " user memberships and cleared all UserMembership records");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to reset memberships: " + e.getMessage());
        }
    }

    // ✅ 테스트용 간단한 엔드포인트
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Admin endpoint working!");
    }

    // ✅ 현재 데이터베이스 상태 확인용 엔드포인트
    @GetMapping("/debug-users")
    public ResponseEntity<String> debugUsers() {
        try {
            List<User> users = userRepository.findAll();
            StringBuilder result = new StringBuilder();
            result.append("=== All Users ===\n");
            
            for (User user : users) {
                result.append("User ID: ").append(user.getId())
                      .append(", Name: ").append(user.getName())
                      .append(", Membership: ").append(user.getMembership())
                      .append(", Memberships collection size: ").append(user.getMemberships().size())
                      .append("\n");
            }
            
            result.append("\n=== Users with Membership ===\n");
            List<User> usersWithMembership = userRepository.findUsersWithMembership();
            for (User user : usersWithMembership) {
                result.append("User ID: ").append(user.getId())
                      .append(", Name: ").append(user.getName())
                      .append(", Membership: ").append(user.getMembership())
                      .append("\n");
            }
            
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ✅ 이벤트별 참석자 목록 조회 API
    @GetMapping("/events/attendees")
    public ResponseEntity<?> getEventAttendees(@RequestParam Long eventId) {
        try {
            List<Map<String, Object>> attendees = eventService.getEventAttendees(eventId);
            return ResponseEntity.ok(attendees);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get attendees: " + e.getMessage());
        }
    }

    // ✅ 이벤트별 참석 통계 조회 API
    @GetMapping("/events/attendance-stats")
    public ResponseEntity<?> getEventAttendanceStats(@RequestParam Long eventId) {
        try {
            Map<String, Object> stats = eventService.getEventAttendanceStats(eventId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get attendance stats: " + e.getMessage());
        }
    }


}