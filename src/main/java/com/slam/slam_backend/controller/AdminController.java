package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.ApplicationDTO;
import com.slam.slam_backend.dto.EventDTO;
import com.slam.slam_backend.entity.Event;
import com.slam.slam_backend.entity.ActionTask;
import com.slam.slam_backend.entity.MembershipApplication;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.UserMembership;
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
            @RequestParam String location,
            @RequestParam(required = false) String description,
            @RequestParam int capacity,
            @RequestParam int price,
            @RequestPart(name = "image", required = false) MultipartFile image
    ) {
        try {
            Event event = new Event();
            event.setBranch(branch);
            event.setTitle(title);
            event.setTheme(theme);
            event.setEventDateTime(java.time.LocalDateTime.parse(eventDateTime));
            event.setLocation(location);
            event.setDescription(description);
            event.setCapacity(capacity);
            event.setPrice(price);
            event.setCurrentAttendees(0);

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
            @RequestParam String location,
            @RequestParam(required = false) String description,
            @RequestParam int capacity,
            @RequestParam int price,
            @RequestPart(name = "image", required = false) MultipartFile image
    ) {
        try {
            Event existingEvent = eventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            existingEvent.setBranch(branch);
            existingEvent.setTitle(title);
            existingEvent.setTheme(theme);
            existingEvent.setEventDateTime(java.time.LocalDateTime.parse(eventDateTime));
            existingEvent.setLocation(location);
            existingEvent.setDescription(description);
            existingEvent.setCapacity(capacity);
            existingEvent.setPrice(price);

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
    public ResponseEntity<?> deleteEvent(@RequestParam Long eventId) {
        try {
            eventRepository.deleteById(eventId);
            return ResponseEntity.ok("Event deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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

    // ✅ 사용자 역할 변경 API (ADMIN 전용, PRESIDENT는 STAFF로만 변경 가능)
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

            String requesterRole = requester.getRole();

            // ADMIN은 모든 역할로 설정 가능, PRESIDENT는 STAFF로만 변경 가능
            if (!"ADMIN".equalsIgnoreCase(requesterRole)) {
                if ("PRESIDENT".equalsIgnoreCase(requesterRole)) {
                    if (!"STAFF".equalsIgnoreCase(role)) {
                        return ResponseEntity.status(403).body("PRESIDENT can only assign STAFF role");
                    }
                } else {
                    return ResponseEntity.status(403).body("Forbidden");
                }
            }

            User target = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            target.setRole(role.toUpperCase());
            userRepository.save(target);

            return ResponseEntity.ok("Role updated to " + target.getRole());
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

    // 모든 게임 목록 조회 (활성화된 것만)
    @GetMapping("/games")
    public ResponseEntity<List<Game>> getAllGames() {
        List<Game> games = gameRepository.findByActiveTrue();
        return ResponseEntity.ok(games);
    }

    // 게임 생성
    @PostMapping("/games")
    public ResponseEntity<Game> createGame(@RequestBody Game game) {
        if (game.getGameId() == null || game.getGameId().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Game saved = gameRepository.save(game);
        return ResponseEntity.ok(saved);
    }

    // 게임 수정
    @PutMapping("/games/{gameId}")
    public ResponseEntity<Game> updateGame(@PathVariable String gameId, @RequestBody Game game) {
        Game existing = gameRepository.findByGameId(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        existing.setName(game.getName());
        existing.setDescription(game.getDescription());
        existing.setCategory(game.getCategory());
        existing.setActive(game.isActive());
        Game saved = gameRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    // 게임 비활성화/삭제
    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<?> deleteGame(@PathVariable String gameId) {
        Game game = gameRepository.findByGameId(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        game.setActive(false);
        gameRepository.save(game);
        return ResponseEntity.ok("Game deactivated");
    }

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

}