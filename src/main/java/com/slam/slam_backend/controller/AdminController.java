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
import com.slam.slam_backend.entity.UserProfile; // UserProfile 임포트
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

import java.util.HashMap;
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

    @GetMapping("/events")
    public ResponseEntity<?> getAllEvents() {
        try {
            List<Event> events = eventRepository.findAll();
            List<EventDTO> eventDTOs = events.stream()
                    .filter(event -> event != null)
                    .map(EventDTO::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(eventDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch events", "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/events", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createEventMultipart(
            @RequestParam String branch, @RequestParam String title, @RequestParam(required = false) String theme,
            @RequestParam String eventDateTime, @RequestParam(required = false) String endTime, @RequestParam String location,
            @RequestParam(required = false) String description, @RequestParam int capacity, @RequestParam int price,
            @RequestParam(required = false) Integer earlyBirdPrice, @RequestParam(required = false) String earlyBirdEndDate,
            @RequestParam(required = false) Integer earlyBirdCapacity, @RequestParam(required = false) String registrationDeadline,
            @RequestParam(required = false) Integer capacityWarningThreshold, @RequestParam(required = false, defaultValue = "false") Boolean showCapacityWarning,
            @RequestParam(required = false) String bankAccount, @RequestPart(name = "image", required = false) MultipartFile image) {
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
            event.setEarlyBirdPrice(earlyBirdPrice);
            event.setEarlyBirdEndDate(earlyBirdEndDate != null && !earlyBirdEndDate.isEmpty() ? java.time.LocalDateTime.parse(earlyBirdEndDate) : null);
            event.setEarlyBirdCapacity(earlyBirdCapacity);
            event.setRegistrationDeadline(registrationDeadline != null && !registrationDeadline.isEmpty() ? java.time.LocalDateTime.parse(registrationDeadline) : null);
            event.setCapacityWarningThreshold(capacityWarningThreshold);
            event.setShowCapacityWarning(showCapacityWarning);
            event.setBankAccount(bankAccount);
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
    
    @PutMapping(value = "/events", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateEventMultipart(
            @RequestParam Long eventId, @RequestParam String branch, @RequestParam String title,
            @RequestParam(required = false) String theme, @RequestParam String eventDateTime,
            @RequestParam(required = false) String endTime, @RequestParam String location,
            @RequestParam(required = false) String description, @RequestParam int capacity, @RequestParam int price,
            @RequestParam(required = false) Integer earlyBirdPrice, @RequestParam(required = false) String earlyBirdEndDate,
            @RequestParam(required = false) Integer earlyBirdCapacity, @RequestParam(required = false) String registrationDeadline,
            @RequestParam(required = false) Integer capacityWarningThreshold, @RequestParam(required = false, defaultValue = "false") Boolean showCapacityWarning,
            @RequestParam(required = false) String bankAccount, @RequestPart(name = "image", required = false) MultipartFile image) {
        try {
            Event existingEvent = eventRepository.findById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));
            existingEvent.setBranch(branch);
            existingEvent.setTitle(title);
            existingEvent.setTheme(theme);
            existingEvent.setEventDateTime(java.time.LocalDateTime.parse(eventDateTime));
            existingEvent.setEndTime(endTime);
            existingEvent.setLocation(location);
            existingEvent.setDescription(description);
            existingEvent.setCapacity(capacity);
            existingEvent.setPrice(price);
            existingEvent.setEarlyBirdPrice(earlyBirdPrice);
            existingEvent.setEarlyBirdEndDate(earlyBirdEndDate != null && !earlyBirdEndDate.isEmpty() ? java.time.LocalDateTime.parse(earlyBirdEndDate) : null);
            existingEvent.setEarlyBirdCapacity(earlyBirdCapacity);
            existingEvent.setRegistrationDeadline(registrationDeadline != null && !registrationDeadline.isEmpty() ? java.time.LocalDateTime.parse(registrationDeadline) : null);
            existingEvent.setCapacityWarningThreshold(capacityWarningThreshold);
            existingEvent.setShowCapacityWarning(showCapacityWarning);
            existingEvent.setBankAccount(bankAccount);
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
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required."));
        }
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null || !isAdmin(user)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다."));
        }
        try {
            eventService.deleteEvent(eventId);
            return ResponseEntity.ok(Map.of("message", "이벤트가 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/events/archive")
    public ResponseEntity<?> archiveEvent(@RequestParam Long eventId) {
        try {
            Event event = eventRepository.findById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));
            event.setArchived(true);
            eventRepository.save(event);
            return ResponseEntity.ok("Event archived successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/users/assign-staff")
    public ResponseEntity<?> assignStaff(@RequestBody StaffAssignmentRequest request, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            String assignerEmail = authentication.getName();
            staffService.initiateStaffAssignment(assignerEmail, request);
            return ResponseEntity.ok(Map.of("success", true, "message", "스태프 임명 이메일이 발송되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/users/role")
    public ResponseEntity<?> updateUserRole(@RequestParam Long userId, @RequestParam String role, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            String requesterEmail = authentication.getName();
            User requester = userRepository.findByEmail(requesterEmail).orElseThrow(() -> new RuntimeException("Requester not found"));
            UserRole requesterRole = requester.getRole();

            if (!requesterRole.canAssignStaff()) {
                return ResponseEntity.status(403).body("You do not have permission to assign staff roles.");
            }
            UserRole targetRole;
            try {
                targetRole = UserRole.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid role: " + role);
            }

            if (!requesterRole.canAssignRole(targetRole)) {
                return ResponseEntity.status(403).body(String.format("%s does not have permission to assign %s role. (Hierarchy violation)", requesterRole.getDisplayName(), targetRole.getDisplayName()));
            }
            User target = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            if (targetRole == UserRole.PRESIDENT) {
                List<User> existingPresidents = userRepository.findByRole(UserRole.PRESIDENT);
                for (User existingPresident : existingPresidents) {
                    if (!existingPresident.getId().equals(target.getId())) {
                        UserRole previousRole = existingPresident.getRole();
                        existingPresident.setRole(UserRole.STAFF);
                        userRepository.save(existingPresident);
                        createRoleChangeNotification(existingPresident, previousRole, UserRole.STAFF, requester);
                    }
                }
            }
            UserRole previousRole = target.getRole();
            target.setRole(targetRole);
            userRepository.save(target);
            createRoleChangeNotification(target, previousRole, targetRole, requester);
            return ResponseEntity.ok("Role updated to " + target.getRole().getDisplayName());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/users/status")
    public ResponseEntity<?> updateUserStatus(@RequestParam Long userId, @RequestParam String status, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            String requesterEmail = authentication.getName();
            User requester = userRepository.findByEmail(requesterEmail).orElseThrow(() -> new RuntimeException("Requester not found"));
            if (!requester.getRole().hasAdminAccess()) {
                return ResponseEntity.status(403).body("You do not have permission.");
            }
            UserStatus targetStatus;
            try {
                targetStatus = UserStatus.valueOf(status.toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid status: " + status);
            }
            User target = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
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
        List<ApplicationDTO> applicationDTOs = applications.stream().map(ApplicationDTO::fromEntity).collect(Collectors.toList());
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

    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationDTO>> getAllApplications() {
        try {
            List<ApplicationDTO> applications = membershipService.findAllApplications();
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(List.of());
        }
    }
    
    @DeleteMapping("/users/memberships")
    public ResponseEntity<?> deleteUserMembership(@RequestParam Long userId, @RequestParam String branchName) {
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            List<UserMembership> memberships = userMembershipRepository.findByUserIdAndBranchName(userId, branchName);
            if (!memberships.isEmpty()) {
                userMembershipRepository.deleteAll(memberships);
            }
            if (branchName.equals(user.getMembership()) || (user.getMembership() != null && user.getMembership().contains(branchName))) {
                user.setMembership(null);
                userRepository.save(user);
            }
            return ResponseEntity.ok("Membership deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers(@RequestParam(name = "sort", required = false, defaultValue = "name") String sort) {
        // UserProfile과 함께 로드하도록 수정
        List<User> users = userRepository.findAllWithProfile();
        if ("name".equalsIgnoreCase(sort)) {
            users.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        } else if ("createdAt".equalsIgnoreCase(sort)) {
            users.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }

        List<Map<String, Object>> userListWithProfile = users.stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            UserProfile profile = user.getUserProfile();

            userMap.put("id", user.getId());
            userMap.put("name", user.getName());
            userMap.put("email", user.getEmail());
            userMap.put("role", user.getRole());
            userMap.put("status", user.getStatus());
            userMap.put("membershipType", user.getMembershipType());
            userMap.put("createdAt", user.getCreatedAt());
            userMap.put("profileImage", user.getProfileImage());
            userMap.put("membership", user.getMembership());

            if (profile != null) {
                userMap.put("affiliation", profile.getAffiliation());
                userMap.put("bio", profile.getBio());
                userMap.put("interests", profile.getInterests());
                userMap.put("spokenLanguages", profile.getSpokenLanguages());
                userMap.put("desiredLanguages", profile.getDesiredLanguages());
                userMap.put("studentId", profile.getStudentId());
                userMap.put("phone", profile.getPhone());
                userMap.put("major", profile.getMajor());
                userMap.put("nationality", profile.getNationality());
                userMap.put("userType", profile.getUserType());
                userMap.put("otherMajor", profile.getOtherMajor());
                userMap.put("professionalStatus", profile.getProfessionalStatus());
                userMap.put("country", profile.getCountry());
                userMap.put("foodAllergies", profile.getFoodAllergies());
                userMap.put("paymentMethod", profile.getPaymentMethod());
                userMap.put("bankLast5", profile.getBankLast5());
                userMap.put("industry", profile.getIndustry());
                userMap.put("networkingGoal", profile.getNetworkingGoal());
                userMap.put("otherNetworkingGoal", profile.getOtherNetworkingGoal());
                
                // 디버깅 로그 추가
                System.out.println("🔍 User: " + user.getName() + ", Nationality: " + profile.getNationality());
            } else {
                System.out.println("🔍 User: " + user.getName() + ", Profile: null");
            }
            return userMap;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(userListWithProfile);
    }
    
    @GetMapping("/users/branch")
    public ResponseEntity<List<Map<String, Object>>> getUsersByBranch(@RequestParam String branchName, @RequestParam(name = "sort", required = false, defaultValue = "name") String sort) {
        try {
            java.util.Map<Long, User> idToUser = new java.util.LinkedHashMap<>();
            List<UserMembership> activeMemberships = userMembershipRepository.findByBranchNameIgnoreCaseAndStatusIgnoreCaseWithProfile(branchName, "ACTIVE");
            for (UserMembership um : activeMemberships) {
                if (um != null && um.getUser() != null && um.getUser().getId() != null) {
                    idToUser.put(um.getUser().getId(), um.getUser());
                }
            }
            List<User> usersFromStringField = userRepository.findByExactMembershipWithProfile(branchName);
            for (User u : usersFromStringField) {
                if (u != null && u.getId() != null) {
                    idToUser.put(u.getId(), u);
                }
            }
            List<User> adminUsers = userRepository.findByRole(UserRole.ADMIN);
            // ADMIN 사용자들의 UserProfile도 로드
            for (User admin : adminUsers) {
                if (admin != null && admin.getId() != null) {
                    // UserProfile이 로드되지 않은 경우 명시적으로 로드
                    if (admin.getUserProfile() == null) {
                        admin = userRepository.findById(admin.getId()).orElse(admin);
                    }
                    idToUser.put(admin.getId(), admin);
                }
            }
            List<User> users = new java.util.ArrayList<>(idToUser.values());
            if ("name".equalsIgnoreCase(sort)) {
                users.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            } else if ("createdAt".equalsIgnoreCase(sort)) {
                users.sort((a, b) -> Long.compare(a.getId(), b.getId()));
            }

            List<Map<String, Object>> withStats = users.stream().map(u -> {
                long joinedCount = eventRsvpRepository.countByUser_IdAndAttendedTrue(u.getId());
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("name", u.getName());
                m.put("email", u.getEmail());
                m.put("membership", u.getMembership());
                m.put("branch", branchName);
                m.put("joinedCount", joinedCount);
                
                // UserProfile에서 nationality 정보 가져오기
                try {
                    if (u.getUserProfile() != null) {
                        String nationality = u.getUserProfile().getNationality();
                        m.put("nationality", nationality);
                        System.out.println("🔍 Branch User: " + u.getName() + ", Nationality: " + nationality);
                    } else {
                        m.put("nationality", null);
                        System.out.println("🔍 Branch User: " + u.getName() + ", Profile: null");
                    }
                } catch (Exception e) {
                    // UserProfile이 로드되지 않은 경우 null로 설정
                    m.put("nationality", null);
                    System.out.println("🔍 Branch User: " + u.getName() + ", Error: " + e.getMessage());
                }
                
                return m;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(withStats);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(List.of(Map.of("error", ex.getMessage())));
        }
    }
    
    @GetMapping("/events/attendees")
    public ResponseEntity<?> getEventAttendees(@RequestParam Long eventId) {
        try {
            List<Map<String, Object>> attendees = eventService.getEventAttendees(eventId);
            return ResponseEntity.ok(attendees);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get attendees: " + e.getMessage());
        }
    }
    
    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(@RequestParam Long eventId, @RequestParam Long userId) {
        try {
            eventService.markAttendance(eventId, userId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // (Game and Finance APIs remain the same)
    
    // Helper Methods
    private void createRoleChangeNotification(User target, UserRole previousRole, UserRole newRole, User changer) {
        String changeType = getChangeType(previousRole, newRole);
        notificationService.createRoleChangeNotification(
            target.getEmail(),
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
        if (newLevel < previousLevel) return "promotion";
        if (newLevel > previousLevel) return "demotion";
        return "change";
    }
    
    private boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.PRESIDENT;
    }

    private void autoSetEventTypeFromTheme(Event event) {
        String theme = event.getTheme();
        if (theme != null) {
            String lowerTheme = theme.toLowerCase();
            if (lowerTheme.contains("regular") && lowerTheme.contains("slam") && lowerTheme.contains("meet")) {
                event.setEventType(EventType.REGULAR_MEET);
                event.setProductType("Membership");
            } else {
                event.setEventType(EventType.SPECIAL_EVENT);
                event.setProductType("Ticket");
            }
        } else {
            event.setEventType(EventType.REGULAR_MEET);
            event.setProductType("Membership");
        }
    }
}