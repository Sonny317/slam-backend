package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.ApplicationDTO;
import com.slam.slam_backend.dto.EventDTO;
import com.slam.slam_backend.dto.StaffAssignmentRequest;
import com.slam.slam_backend.entity.Event;
import com.slam.slam_backend.entity.EventType;
import com.slam.slam_backend.entity.ActionTask;
import com.slam.slam_backend.entity.MembershipApplication;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.UserMembership;
import com.slam.slam_backend.entity.UserRole;
import com.slam.slam_backend.entity.UserStatus;
import com.slam.slam_backend.repository.EventRepository;
import com.slam.slam_backend.repository.EventRsvpRepository;
import com.slam.slam_backend.repository.MembershipApplicationRepository;
import com.slam.slam_backend.repository.UserMembershipRepository;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.repository.ActionTaskRepository;
import com.slam.slam_backend.repository.GameRepository;
import com.slam.slam_backend.repository.EventGameRepository;
import com.slam.slam_backend.entity.Game;
import com.slam.slam_backend.entity.EventGame;
import com.slam.slam_backend.entity.FinanceTransaction;
import com.slam.slam_backend.service.EventService;
import com.slam.slam_backend.service.MembershipService;
import com.slam.slam_backend.service.StaffService;
import com.slam.slam_backend.service.GameService;
import com.slam.slam_backend.service.GameAnalyticsService;
import com.slam.slam_backend.service.NotificationService;
import com.slam.slam_backend.dto.GameCreateRequest;
import com.slam.slam_backend.dto.GameFeedbackCreateRequest;
import com.slam.slam_backend.dto.GameAnalyticsDTO;
import com.slam.slam_backend.entity.GameFeedback;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MembershipService membershipService;
    private final EventService eventService;
    private final StaffService staffService;
    private final GameService gameService;
    private final GameAnalyticsService gameAnalyticsService;
    private final NotificationService notificationService;
    private final MembershipApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final EventRepository eventRepository;
    private final EventRsvpRepository eventRsvpRepository;
  private final ActionTaskRepository actionTaskRepository;
    private final GameRepository gameRepository;
    private final EventGameRepository eventGameRepository;
    private final com.slam.slam_backend.repository.FinanceTransactionRepository financeTransactionRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

  // ✅ 영수증 업로드 (최대 1MB 정도로 프론트에서 제한, 서버는 파일 저장만 수행)
  @PostMapping(value = "/receipts/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadReceipt(@RequestPart("file") MultipartFile file) {
      try {
          if (file == null || file.isEmpty()) {
              return ResponseEntity.badRequest().body("Empty file");
          }
          String original = file.getOriginalFilename();
          String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf('.')) : "";
          String filename = java.util.UUID.randomUUID() + ext;
          java.io.File dir = new java.io.File(uploadDir);
          if (!dir.exists()) dir.mkdirs();
          java.io.File dest = new java.io.File(dir, filename);
          file.transferTo(dest);
          return ResponseEntity.ok(java.util.Map.of("url", "/images/" + filename));
      } catch (Exception e) {
          return ResponseEntity.badRequest().body("Failed to upload receipt: " + e.getMessage());
      }
  }

  // === Action Plan APIs ===
  @GetMapping("/actions")
  public ResponseEntity<?> listActions(@RequestParam(required = false) String branch,
                                       @RequestParam(required = false, defaultValue = "false") boolean archived) {
      java.util.List<ActionTask> all = actionTaskRepository.findAll();
      java.util.stream.Stream<ActionTask> s = all.stream();
      if (branch != null && !branch.isEmpty()) {
          s = s.filter(t -> branch.equalsIgnoreCase(t.getBranch()));
      }
      if (!archived) {
          s = s.filter(t -> !t.isArchived());
      }
      return ResponseEntity.ok(s.toList());
  }

  @PostMapping("/actions")
  public ResponseEntity<?> createAction(@RequestBody ActionTask task) {
      if (task.getStatus() == null || task.getStatus().isEmpty()) task.setStatus("todo");
      if (task.getBranch() == null || task.getBranch().isEmpty()) task.setBranch("NCCU");
      return ResponseEntity.ok(actionTaskRepository.save(task));
  }

  @PutMapping("/actions/{id}")
  public ResponseEntity<?> updateAction(@PathVariable Long id, @RequestBody ActionTask payload) {
      ActionTask t = actionTaskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
      t.setTitle(payload.getTitle());
      t.setTeam(payload.getTeam());
      t.setAgenda(payload.getAgenda());
      t.setDeadline(payload.getDeadline());
      t.setStatus(payload.getStatus());
      if (payload.getBranch() != null) t.setBranch(payload.getBranch());
      if (payload.getEventTitle() != null) t.setEventTitle(payload.getEventTitle());
      t.setArchived(payload.isArchived());
      t.setUpdatedAt(java.time.LocalDateTime.now());
      return ResponseEntity.ok(actionTaskRepository.save(t));
  }

  @DeleteMapping("/actions/{id}")
  public ResponseEntity<?> deleteAction(@PathVariable Long id) {
      actionTaskRepository.deleteById(id);
      return ResponseEntity.ok().build();
  }

  @PostMapping("/actions/{id}/ack-toggle")
  public ResponseEntity<?> toggleAck(@PathVariable Long id, Authentication authentication) {
      if (authentication == null) return ResponseEntity.status(401).body("Unauthorized");
      String email = authentication.getName();
      User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
      ActionTask t = actionTaskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
      boolean removed = t.getAcknowledgedBy().removeIf(u -> u.getId().equals(user.getId()));
      if (!removed) t.getAcknowledgedBy().add(user);
      return ResponseEntity.ok(actionTaskRepository.save(t));
  }

    // ✅ 이벤트 관리 API들
    @GetMapping("/events")
    public ResponseEntity<?> getAllEvents() {
        try {
            System.out.println("AdminPage API called - Fetching all events");
            
            List<Event> events = eventRepository.findAll();
            System.out.println("Found " + events.size() + " events in database");
            
            if (events.isEmpty()) {
                System.out.println("No events found, returning empty list");
                return ResponseEntity.ok(new java.util.ArrayList<>());
            }
            
            List<EventDTO> eventDTOs = events.stream()
                    .filter(event -> event != null) // null 체크 추가
                    .map(event -> {
                        try {
                            return EventDTO.fromEntity(event);
                        } catch (Exception e) {
                            System.err.println("Error converting event " + event.getId() + " to DTO: " + e.getMessage());
                            e.printStackTrace();
                            return null;
                        }
                    })
                    .filter(dto -> dto != null) // null DTO 제거
                    .collect(Collectors.toList());
                    
            System.out.println("AdminPage API called - Total events: " + eventDTOs.size());
            System.out.println("Events: " + eventDTOs.stream().map(e -> e.getTitle() + " (" + e.getBranch() + ")").collect(Collectors.toList()));
            
            return ResponseEntity.ok(eventDTOs);
        } catch (Exception e) {
            System.err.println("Error in getAllEvents: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch events",
                "message", e.getMessage() != null ? e.getMessage() : "Unknown error",
                "type", e.getClass().getSimpleName()
            ));
        }
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

    // ✅ 멀티파트 업로드로 이벤트 생성 (이미지 파일 업로드 지원)
    @PostMapping(value = "/events", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createEventMultipart(
            @RequestParam String branch,
            @RequestParam String title,
            @RequestParam(required = false) String theme,
            @RequestParam String eventDateTime,
            @RequestParam(required = false) String endTime,
            @RequestParam String location,
            @RequestParam(required = false) String description,
            @RequestParam int capacity,
            @RequestParam int price,
            // ✅ Early Bird 관련 필드들
            @RequestParam(required = false) Integer earlyBirdPrice,
            @RequestParam(required = false) String earlyBirdEndDate,
            @RequestParam(required = false) Integer earlyBirdCapacity,
            // ✅ 등록 데드라인
            @RequestParam(required = false) String registrationDeadline,
            // ✅ 용량 경고 설정
            @RequestParam(required = false) Integer capacityWarningThreshold,
            @RequestParam(required = false, defaultValue = "false") Boolean showCapacityWarning,
            // ✅ 계좌 정보
            @RequestParam(required = false) String bankAccount,
            @RequestPart(name = "image", required = false) MultipartFile image
    ) {
        try {
            Event event = new Event();
            event.setBranch(branch);
            event.setTitle(title);
            event.setTheme(theme);
            event.setEventDateTime(java.time.LocalDateTime.parse(eventDateTime));
            event.setEndTime(endTime);
            event.setLocation(location);
            event.setDescription(description);
            event.setCapacity(capacity);
            event.setPrice(price);
            event.setCurrentAttendees(0);
            event.setArchived(false);
            
            // ✅ Early Bird 관련 필드들 설정
            event.setEarlyBirdPrice(earlyBirdPrice);
            event.setEarlyBirdEndDate(earlyBirdEndDate != null && !earlyBirdEndDate.isEmpty() 
                ? java.time.LocalDateTime.parse(earlyBirdEndDate) : null);
            event.setEarlyBirdCapacity(earlyBirdCapacity);
            
            // ✅ 등록 데드라인 설정
            event.setRegistrationDeadline(registrationDeadline != null && !registrationDeadline.isEmpty() 
                ? java.time.LocalDateTime.parse(registrationDeadline) : null);
                
            // ✅ 용량 경고 설정
            event.setCapacityWarningThreshold(capacityWarningThreshold);
            event.setShowCapacityWarning(showCapacityWarning);
            
            // ✅ 계좌 정보 설정
            event.setBankAccount(bankAccount);
            
            // ✅ Theme 기반으로 EventType과 ProductType 자동 설정
            autoSetEventTypeFromTheme(event);

            if (image != null && !image.isEmpty()) {
                String original = image.getOriginalFilename();
                String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf('.')) : "";
                String filename = java.util.UUID.randomUUID() + ext;
                java.io.File dir = new java.io.File(uploadDir);
                if (!dir.exists()) dir.mkdirs();
                java.io.File dest = new java.io.File(dir, filename);
                image.transferTo(dest);
                event.setImageUrl("/images/" + filename);
            }

            Event saved = eventRepository.save(event);
            return ResponseEntity.ok(EventDTO.fromEntity(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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

    // ✅ 멀티파트 업로드로 이벤트 수정 (이미지 파일 교체 가능)
    @PutMapping(value = "/events", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateEventMultipart(
            @RequestParam Long eventId,
            @RequestParam String branch,
            @RequestParam String title,
            @RequestParam(required = false) String theme,
            @RequestParam String eventDateTime,
            @RequestParam(required = false) String endTime,
            @RequestParam String location,
            @RequestParam(required = false) String description,
            @RequestParam int capacity,
            @RequestParam int price,
            // ✅ Early Bird 관련 필드들
            @RequestParam(required = false) Integer earlyBirdPrice,
            @RequestParam(required = false) String earlyBirdEndDate,
            @RequestParam(required = false) Integer earlyBirdCapacity,
            // ✅ 등록 데드라인
            @RequestParam(required = false) String registrationDeadline,
            // ✅ 용량 경고 설정
            @RequestParam(required = false) Integer capacityWarningThreshold,
            @RequestParam(required = false, defaultValue = "false") Boolean showCapacityWarning,
            // ✅ 계좌 정보
            @RequestParam(required = false) String bankAccount,
            @RequestPart(name = "image", required = false) MultipartFile image
    ) {
        try {
            Event existingEvent = eventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            existingEvent.setBranch(branch);
            existingEvent.setTitle(title);
            existingEvent.setTheme(theme);
            existingEvent.setEventDateTime(java.time.LocalDateTime.parse(eventDateTime));
            existingEvent.setEndTime(endTime);
            existingEvent.setLocation(location);
            existingEvent.setDescription(description);
            existingEvent.setCapacity(capacity);
            existingEvent.setPrice(price);
            
            // ✅ Early Bird 관련 필드들 설정
            existingEvent.setEarlyBirdPrice(earlyBirdPrice);
            existingEvent.setEarlyBirdEndDate(earlyBirdEndDate != null && !earlyBirdEndDate.isEmpty() 
                ? java.time.LocalDateTime.parse(earlyBirdEndDate) : null);
            existingEvent.setEarlyBirdCapacity(earlyBirdCapacity);
            
            // ✅ 등록 데드라인 설정
            existingEvent.setRegistrationDeadline(registrationDeadline != null && !registrationDeadline.isEmpty() 
                ? java.time.LocalDateTime.parse(registrationDeadline) : null);
                
            // ✅ 용량 경고 설정
            existingEvent.setCapacityWarningThreshold(capacityWarningThreshold);
            existingEvent.setShowCapacityWarning(showCapacityWarning);
            
            // ✅ 계좌 정보 설정
            existingEvent.setBankAccount(bankAccount);
            
            // ✅ Theme 기반으로 EventType과 ProductType 자동 설정
            autoSetEventTypeFromTheme(existingEvent);

            if (image != null && !image.isEmpty()) {
                String original = image.getOriginalFilename();
                String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf('.')) : "";
                String filename = java.util.UUID.randomUUID() + ext;
                java.io.File dir = new java.io.File(uploadDir);
                if (!dir.exists()) dir.mkdirs();
                java.io.File dest = new java.io.File(dir, filename);
                image.transferTo(dest);
                existingEvent.setImageUrl("/images/" + filename);
            }

            Event saved = eventRepository.save(existingEvent);
            return ResponseEntity.ok(EventDTO.fromEntity(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/events")
    public ResponseEntity<?> deleteEvent(@RequestParam Long eventId, Authentication authentication) {
        System.out.println("🗑️ 이벤트 삭제 요청 - Event ID: " + eventId);
        
        if (authentication == null) {
            System.out.println("❌ 인증 실패 - Authentication is null");
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        // 관리자 권한 체크
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null || !isAdmin(user)) {
            System.out.println("❌ 권한 실패 - User: " + userEmail + ", Role: " + (user != null ? user.getRole() : "null"));
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }
        
        try {
            System.out.println("🔄 이벤트 삭제 시작 - Event ID: " + eventId);
            eventService.deleteEvent(eventId);  // ✅ EventService 사용으로 안전한 삭제
            System.out.println("✅ 이벤트 삭제 완료 - Event ID: " + eventId);
            return ResponseEntity.ok(Map.of("message", "이벤트가 삭제되었습니다."));
        } catch (Exception e) {
            System.out.println("❌ 이벤트 삭제 실패 - Event ID: " + eventId + ", Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ 이벤트 보관처리(과거로 이동) API
    @PostMapping("/events/archive")
    public ResponseEntity<?> archiveEvent(@RequestParam Long eventId) {
        try {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            event.setArchived(true);
            eventRepository.save(event);
            return ResponseEntity.ok("Event archived successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ 스태프 임명 API (PRESIDENT/ADMIN 전용) - 온보딩 프로세스 시작
    @PostMapping("/users/assign-staff")
    public ResponseEntity<?> assignStaff(@RequestBody StaffAssignmentRequest request,
                                        Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

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

    // ✅ 사용자 역할 변경 API (기존 방식, 간단한 역할 변경용)
    @PostMapping("/users/role")
    public ResponseEntity<?> updateUserRole(@RequestParam Long userId,
                                            @RequestParam String role,
                                            Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

            String requesterEmail = authentication.getName();
            User requester = userRepository.findByEmail(requesterEmail)
                    .orElseThrow(() -> new RuntimeException("Requester not found"));

            UserRole requesterRole = requester.getRole();

            // 권한 검증: 스태프 임명 권한 확인
            if (!requesterRole.canAssignStaff()) {
                return ResponseEntity.status(403).body("You do not have permission to assign staff roles.");
            }

            // 역할 문자열을 enum으로 변환
            UserRole targetRole;
            try {
                targetRole = UserRole.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid role: " + role);
            }

            // 계층 구조 기반 권한 검증
            if (!requesterRole.canAssignRole(targetRole)) {
                return ResponseEntity.status(403).body(
                    String.format("%s does not have permission to assign %s role. (Hierarchy violation)", 
                                requesterRole.getDisplayName(), 
                                targetRole.getDisplayName())
                );
            }

            User target = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // ✅ President를 다른 사용자에게 임명하는 경우, 기존 President들을 Staff로 변경
            if (targetRole == UserRole.PRESIDENT) {
                List<User> existingPresidents = userRepository.findByRole(UserRole.PRESIDENT);
                for (User existingPresident : existingPresidents) {
                    if (!existingPresident.getId().equals(target.getId())) {
                        UserRole previousRole = existingPresident.getRole();
                        existingPresident.setRole(UserRole.STAFF);
                        userRepository.save(existingPresident);
                        
                        // 기존 President에게 강등 알림 생성
                        createRoleChangeNotification(existingPresident, previousRole, UserRole.STAFF, requester);
                        
                        System.out.println("기존 President " + existingPresident.getName() + "를 Staff로 변경했습니다.");
                    }
                }
            }

            // 기존 역할 저장 (알림용)
            UserRole previousRole = target.getRole();
            
            System.out.println("🎯 역할 변경 시작:");
            System.out.println("  - 대상 사용자: " + target.getName() + " (ID: " + target.getId() + ")");
            System.out.println("  - 이전 역할: " + previousRole);
            System.out.println("  - 새로운 역할: " + targetRole);
            System.out.println("  - 변경자: " + requester.getName() + " (ID: " + requester.getId() + ")");
            
            // 대상 사용자의 역할 변경
            target.setRole(targetRole);
            userRepository.save(target);

            // 역할 변경 알림 생성
            try {
                System.out.println("🔔 알림 생성 메서드 호출 시작...");
                createRoleChangeNotification(target, previousRole, targetRole, requester);
                System.out.println("✅ 알림 생성 성공: " + target.getName() + " (" + previousRole + " → " + targetRole + ")");
            } catch (Exception e) {
                System.err.println("❌ 알림 생성 실패: " + e.getMessage());
                e.printStackTrace();
            }

            return ResponseEntity.ok("Role updated to " + target.getRole().getDisplayName());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ 사용자 상태 변경 API (멤버십 승인 등)
    @PostMapping("/users/status")
    public ResponseEntity<?> updateUserStatus(@RequestParam Long userId,
                                              @RequestParam String status,
                                              Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

            String requesterEmail = authentication.getName();
            User requester = userRepository.findByEmail(requesterEmail)
                    .orElseThrow(() -> new RuntimeException("Requester not found"));

            // 관리자 권한 확인
            if (!requester.getRole().hasAdminAccess()) {
                return ResponseEntity.status(403).body("You do not have permission.");
            }

            // 상태 문자열을 enum으로 변환
            UserStatus targetStatus;
            try {
                targetStatus = UserStatus.valueOf(status.toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid status: " + status);
            }

            User target = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            target.setStatus(targetStatus);
            userRepository.save(target);

            return ResponseEntity.ok("Status updated to " + target.getStatus().getDisplayName());
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
    public ResponseEntity<List<User>> getAllUsers(@RequestParam(name = "sort", required = false, defaultValue = "name") String sort) {
        List<User> users = userRepository.findAll();
        if ("name".equalsIgnoreCase(sort)) {
            users.sort((a, b) -> {
                String an = a.getName() == null ? "" : a.getName();
                String bn = b.getName() == null ? "" : b.getName();
                return an.compareToIgnoreCase(bn);
            });
        } else if ("createdAt".equalsIgnoreCase(sort)) {
            // createdAt 필드가 User에 없으므로, id 순(근사치)으로 정렬
            users.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        }
        return ResponseEntity.ok(users);
    }

    // ✅ 특정 지부의 멤버 목록 조회 API (정렬 지원)
    @GetMapping("/users/branch")
    public ResponseEntity<List<Map<String, Object>>> getUsersByBranch(@RequestParam String branchName,
                                                       @RequestParam(name = "sort", required = false, defaultValue = "name") String sort) {
        try {
            // 지부별 ACTIVE 멤버십 기반으로 사용자 수집 (대소문자 무시)
            List<UserMembership> activeMemberships = userMembershipRepository.findByBranchNameIgnoreCaseAndStatusIgnoreCase(branchName, "ACTIVE");
            System.out.println("[Users/Branch] branch=" + branchName + ", activeMemberships=" + activeMemberships.size());

            // ID 기준 dedupe를 위해 LinkedHashMap 사용
            java.util.Map<Long, User> idToUser = new java.util.LinkedHashMap<>();
            for (UserMembership um : activeMemberships) {
                if (um != null && um.getUser() != null && um.getUser().getId() != null) {
                    idToUser.put(um.getUser().getId(), um.getUser());
                }
            }

            // 단일 문자열 membership 필드 기반 폴백(과거 데이터 호환)
            List<User> usersFromStringField = userRepository.findByMembershipContaining(branchName);
            System.out.println("[Users/Branch] usersFromStringField=" + usersFromStringField.size());
            for (User u : usersFromStringField) {
                if (u != null && u.getId() != null) {
                    idToUser.put(u.getId(), u);
                }
            }

            List<User> users = new java.util.ArrayList<>(idToUser.values());

            if ("name".equalsIgnoreCase(sort)) {
                users.sort((a, b) -> {
                    String an = a.getName() == null ? "" : a.getName();
                    String bn = b.getName() == null ? "" : b.getName();
                    return an.compareToIgnoreCase(bn);
                });
            } else if ("createdAt".equalsIgnoreCase(sort)) {
                // createdAt 필드가 없으므로 id를 근사치로 사용
                users.sort((a, b) -> Long.compare(a.getId(), b.getId()));
            }

            // 사용자별 참여 횟수(joinedCount) 포함 응답 구성
            List<Map<String, Object>> withStats = users.stream().map(u -> {
                long joinedCount = eventRsvpRepository.countByUser_IdAndAttendedTrue(u.getId());
                String membership = u.getMembership();
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", u.getId());
                m.put("name", u.getName());
                m.put("email", u.getEmail());
                m.put("membership", membership); // null 허용
                m.put("branch", branchName);
                m.put("joinedCount", joinedCount);
                return m;
            }).collect(java.util.stream.Collectors.toList());

            System.out.println("[Users/Branch] response size=" + withStats.size());
            return ResponseEntity.ok(withStats);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(java.util.List.of(java.util.Map.of(
                "error", ex.getClass().getSimpleName(),
                "message", ex.getMessage() == null ? "null" : ex.getMessage(),
                "branch", branchName
            )));
        }
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
    public ResponseEntity<Map<String, Object>> testEndpoint() {
        try {
            // 데이터베이스 연결 테스트
            long userCount = userRepository.count();
            long eventCount = eventRepository.count();
            
            Map<String, Object> status = new java.util.HashMap<>();
            status.put("status", "OK");
            status.put("timestamp", java.time.LocalDateTime.now().toString());
            status.put("userCount", userCount);
            status.put("eventCount", eventCount);
            status.put("database", "Connected");
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("status", "ERROR");
            error.put("timestamp", java.time.LocalDateTime.now().toString());
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            
            return ResponseEntity.status(500).body(error);
        }
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

    // ✅ QR 체크인: 참석 완료(attended) 기록
    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(@RequestParam Long eventId, @RequestParam Long userId) {
        try {
            eventService.markAttendance(eventId, userId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // === Game Management APIs ===

    // 게임 생성 (OLD - 주석 처리됨, 새로운 API 사용)
    // @PostMapping("/games")
    // public ResponseEntity<Game> createGame(@RequestBody Game game) {
    //     if (game.getGameId() == null || game.getGameId().isEmpty()) {
    //         return ResponseEntity.badRequest().build();
    //     }
    //     Game saved = gameRepository.save(game);
    //     return ResponseEntity.ok(saved);
    // }

    // 게임 수정 (OLD - 주석 처리됨, 새로운 API 사용)
    // @PutMapping("/games/{gameId}")
    // public ResponseEntity<Game> updateGame(@PathVariable String gameId, @RequestBody Game game) {
    //     Game existing = gameRepository.findByGameId(gameId)
    //             .orElseThrow(() -> new RuntimeException("Game not found"));
    //     existing.setName(game.getName());
    //     existing.setDescription(game.getDescription());
    //     existing.setCategory(game.getCategory());
    //     existing.setActive(game.isActive());
    //     Game saved = gameRepository.save(existing);
    //     return ResponseEntity.ok(saved);
    // }

    // 게임 비활성화/삭제 (OLD - 주석 처리됨, 새로운 API 사용)
    // @DeleteMapping("/games/{gameId}")
    // public ResponseEntity<?> deleteGame(@PathVariable String gameId) {
    //     Game game = gameRepository.findByGameId(gameId)
    //             .orElseThrow(() -> new RuntimeException("Game not found"));
    //     game.setActive(false);
    //     gameRepository.save(game);
    //     return ResponseEntity.ok("Game deactivated");
    // }

    // 특정 이벤트에 게임 할당
    @PostMapping("/events/{eventId}/games")
    public ResponseEntity<?> assignGamesToEvent(@PathVariable Long eventId, @RequestBody List<String> gameIds) {
        // 기존 게임 할당 삭제
        eventGameRepository.deleteByEventId(eventId);
        
        // 새로운 게임 할당
        for (String gameId : gameIds) {
            EventGame eventGame = new EventGame(eventId, gameId);
            eventGameRepository.save(eventGame);
        }
        return ResponseEntity.ok("Games assigned to event");
    }

    // 특정 이벤트의 게임 목록 조회
    @GetMapping("/events/{eventId}/games")
    public ResponseEntity<List<Map<String, Object>>> getEventGames(@PathVariable Long eventId) {
        List<EventGame> eventGames = eventGameRepository.findByEventId(eventId);
        List<Map<String, Object>> result = eventGames.stream().map(eg -> {
            Game game = gameRepository.findByGameId(eg.getGameId()).orElse(null);
            Map<String, Object> gameInfo = new java.util.HashMap<>();
            gameInfo.put("gameId", eg.getGameId());
            gameInfo.put("name", game != null ? game.getName() : "Unknown Game");
            gameInfo.put("category", game != null ? game.getCategory() : "Unknown");
            return gameInfo;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // === Finance (Accounting) APIs ===
    @GetMapping("/finance")
    public ResponseEntity<?> listFinance(@RequestParam String branch) {
        try {
            java.util.List<FinanceTransaction> list = financeTransactionRepository.findByBranchNameIgnoreCaseOrderByDateDescIdDesc(branch);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to list finance: " + e.getMessage());
        }
    }

    @PostMapping("/finance")
    public ResponseEntity<?> createFinance(@RequestBody FinanceTransaction payload, Authentication authentication) {
        try {
            if (payload.getBranchName() == null || payload.getBranchName().isEmpty()) {
                return ResponseEntity.badRequest().body("branchName is required");
            }
            if (payload.getType() == null || payload.getDate() == null || payload.getItem() == null || payload.getAmount() == null) {
                return ResponseEntity.badRequest().body("type/date/item/amount are required");
            }
            if (authentication != null && (payload.getSubmittedBy() == null || payload.getSubmittedBy().isEmpty())) {
                String email = authentication.getName();
                com.slam.slam_backend.entity.User u = userRepository.findByEmail(email).orElse(null);
                if (u != null) payload.setSubmittedBy(u.getName() != null ? u.getName() : u.getEmail());
                else payload.setSubmittedBy(email);
            }
            FinanceTransaction saved = financeTransactionRepository.save(payload);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create finance: " + e.getMessage());
        }
    }

    @PutMapping("/finance/{id}")
    public ResponseEntity<?> updateFinance(@PathVariable Long id, @RequestBody FinanceTransaction payload) {
        try {
            FinanceTransaction t = financeTransactionRepository.findById(id).orElseThrow(() -> new RuntimeException("Finance not found"));
            if (payload.getType() != null) t.setType(payload.getType());
            if (payload.getDate() != null) t.setDate(payload.getDate());
            if (payload.getItem() != null) t.setItem(payload.getItem());
            if (payload.getAmount() != null) t.setAmount(payload.getAmount());
            if (payload.getEventTitle() != null) t.setEventTitle(payload.getEventTitle());
            if (payload.getReceiptUrl() != null) t.setReceiptUrl(payload.getReceiptUrl());
            if (payload.getSubmittedBy() != null) t.setSubmittedBy(payload.getSubmittedBy());
            if (payload.getStatus() != null) t.setStatus(payload.getStatus());
            return ResponseEntity.ok(financeTransactionRepository.save(t));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update finance: " + e.getMessage());
        }
    }

    @DeleteMapping("/finance/{id}")
    public ResponseEntity<?> deleteFinance(@PathVariable Long id) {
        try {
            financeTransactionRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete finance: " + e.getMessage());
        }
    }

    // ============== 게임 관리 API ==============

    /**
     * 모든 게임 조회 (관리자용)
     */
    @GetMapping("/games")
    public ResponseEntity<List<Game>> getAllGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }

    /**
     * 활성 게임만 조회
     */
    @GetMapping("/games/active")
    public ResponseEntity<List<Game>> getActiveGames() {
        return ResponseEntity.ok(gameService.getAllActiveGames());
    }

    /**
     * 새 게임 생성
     */
    @PostMapping("/games")
    public ResponseEntity<?> createGame(@RequestBody GameCreateRequest request) {
        try {
            Game game = gameService.createGame(request);
            return ResponseEntity.ok(game);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create game: " + e.getMessage());
        }
    }

    /**
     * 게임 정보 수정
     */
    @PutMapping("/games/{gameId}")
    public ResponseEntity<?> updateGame(@PathVariable String gameId, @RequestBody GameCreateRequest request) {
        try {
            Game game = gameService.updateGame(gameId, request);
            return ResponseEntity.ok(game);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update game: " + e.getMessage());
        }
    }

    /**
     * 게임 활성화/비활성화 토글
     */
    @PatchMapping("/games/{gameId}/toggle")
    public ResponseEntity<?> toggleGameActive(@PathVariable String gameId) {
        try {
            Game game = gameService.toggleGameActive(gameId);
            return ResponseEntity.ok(game);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to toggle game: " + e.getMessage());
        }
    }

    /**
     * 특정 게임의 상세 분석 조회
     */
    @GetMapping("/games/{gameId}/analytics")
    public ResponseEntity<?> getGameAnalytics(@PathVariable String gameId) {
        try {
            GameAnalyticsDTO analytics = gameAnalyticsService.analyzeGame(gameId);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get analytics: " + e.getMessage());
        }
    }

    /**
     * 모든 게임의 분석 요약 조회
     */
    @GetMapping("/games/analytics")
    public ResponseEntity<?> getAllGameAnalytics() {
        try {
            List<GameAnalyticsDTO> analytics = gameAnalyticsService.analyzeAllGames();
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get analytics: " + e.getMessage());
        }
    }

    /**
     * 게임 피드백 생성 (관리자용)
     */
    @PostMapping("/games/feedback")
    public ResponseEntity<?> createGameFeedback(@RequestBody GameFeedbackCreateRequest request, Authentication authentication) {
        try {
            String submittedBy = "admin-" + authentication.getName();
            GameFeedback feedback = gameService.createGameFeedback(request, submittedBy);
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create feedback: " + e.getMessage());
        }
    }

    /**
     * 특정 이벤트의 게임 피드백 조회
     */
    @GetMapping("/events/{eventId}/game-feedbacks")
    public ResponseEntity<?> getEventGameFeedbacks(@PathVariable Long eventId) {
        try {
            List<GameFeedback> feedbacks = gameService.getGameFeedbacksByEvent(eventId);
            return ResponseEntity.ok(feedbacks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get feedbacks: " + e.getMessage());
        }
    }

    /**
     * 특정 게임의 모든 피드백 조회
     */
    @GetMapping("/games/{gameId}/feedbacks")
    public ResponseEntity<?> getGameFeedbacks(@PathVariable String gameId) {
        try {
            List<GameFeedback> feedbacks = gameService.getGameFeedbacksByGame(gameId);
            return ResponseEntity.ok(feedbacks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get feedbacks: " + e.getMessage());
        }
    }

    /**
     * 역할 변경 알림 생성
     */
    private void createRoleChangeNotification(User target, UserRole previousRole, UserRole newRole, User changer) {
        String changeType = getChangeType(previousRole, newRole);
        String message = String.format("Your role has been changed from %s to %s by %s", 
                                      previousRole.getDisplayName(), 
                                      newRole.getDisplayName(), 
                                      changer.getName());
        
        System.out.println("🔧 알림 생성 - 사용자 이메일로 저장: " + target.getEmail());
        
        notificationService.createRoleChangeNotification(
            target.getEmail(),  // ✅ 이메일로 변경 (ID 대신)
            changer.getName(),
            previousRole.getDisplayName(),
            newRole.getDisplayName(),
            changeType,
            changer.getId()
        );
    }

    private String getChangeType(UserRole previousRole, UserRole newRole) {
        int previousLevel = previousRole.getHierarchyLevel();
        int newLevel = newRole.getHierarchyLevel();
        
        if (newLevel < previousLevel) {
            return "promotion"; // 숫자가 작을수록 높은 권한
        } else if (newLevel > previousLevel) {
            return "demotion"; // 숫자가 클수록 낮은 권한
        } else {
            return "change"; // 같은 레벨
        }
    }

    private boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.PRESIDENT;
    }
    
    private void autoSetEventTypeFromTheme(Event event) {
        String theme = event.getTheme();
        if (theme != null) {
            String lowerTheme = theme.toLowerCase();
            
            // Regular SLAM Meet인 경우
            if (lowerTheme.contains("regular") && lowerTheme.contains("slam") && lowerTheme.contains("meet")) {
                event.setEventType(EventType.REGULAR_MEET);
                event.setProductType("Membership");
            } 
            // 그 외 모든 테마는 Special Event
            else {
                event.setEventType(EventType.SPECIAL_EVENT);
                event.setProductType("Ticket");
            }
        } else {
            // Theme이 없으면 기본값
            event.setEventType(EventType.REGULAR_MEET);
            event.setProductType("Membership");
        }
    }

}