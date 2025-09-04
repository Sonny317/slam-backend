package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.EventDTO;
import com.slam.slam_backend.dto.RsvpRequest;
import com.slam.slam_backend.dto.MembershipRequest;
import com.slam.slam_backend.service.EventService;
import com.slam.slam_backend.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.slam.slam_backend.entity.EventRsvp;

import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths; // ✅ 임포트 추가
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional; // ✅ 임포트 추가
import java.util.stream.Collectors; // ✅ Collectors 임포트 추가

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final MembershipService membershipService;

    @Value("${file.upload-dir}") // ✅ application.properties에서 경로를 가져옵니다.
    private String uploadDir;

    @GetMapping
    public ResponseEntity<List<EventDTO>> getAllEvents(
            @RequestParam(required = false) String branch,
            Authentication authentication) {
        
        String userEmail = null;
        if (authentication != null && authentication.isAuthenticated()) {
            userEmail = authentication.getName();
        }
        
        List<EventDTO> events = eventService.findAllEventsForUser(branch, userEmail);
        System.out.println("EventsPage API called - Total events: " + events.size());
        System.out.println("Events: " + events.stream().map(e -> e.getTitle() + " (" + e.getBranch() + ")").collect(Collectors.toList()));
        return ResponseEntity.ok(events);
    }

    @GetMapping("/detail")
    public ResponseEntity<EventDTO> getEventById(@RequestParam Long eventId) {
        try {
            EventDTO event = eventService.findEventById(eventId);
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ 이벤트 참석 상태 확인 API
    @GetMapping("/{eventId}/attendance-status")
    public ResponseEntity<?> getAttendanceStatus(@PathVariable Long eventId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Login required.");
        }

        try {
            String userEmail = authentication.getName();
            
            // 1. RSVP 상태 확인
            boolean hasRsvp = eventService.hasUserRsvp(eventId, userEmail);
            
            // 2. 승인 대기 중인 티켓 구매 신청 확인
            boolean hasPendingTicket = membershipService.hasPendingTicketApplication(userEmail, eventId);
            
            // ✅ 디버깅 로그 추가
            System.out.println("🔍 Attendance Status Debug - Event ID: " + eventId + ", User: " + userEmail);
            System.out.println("   - hasRsvp: " + hasRsvp);
            System.out.println("   - hasPendingTicket: " + hasPendingTicket);
            
            Map<String, Object> status = new HashMap<>();
            status.put("hasRsvp", hasRsvp);
            status.put("hasPendingTicket", hasPendingTicket);
            
            if (hasRsvp) {
                status.put("status", "ATTENDING");
                System.out.println("   - Final Status: ATTENDING");
            } else if (hasPendingTicket) {
                status.put("status", "PENDING_APPROVAL");
                System.out.println("   - Final Status: PENDING_APPROVAL");
            } else {
                status.put("status", "NOT_ATTENDING");
                System.out.println("   - Final Status: NOT_ATTENDING");
            }
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error checking status: " + e.getMessage());
        }
    }

    // --- ⬇️ 문제 진단을 위한 테스트 API ⬇️ ---
    @GetMapping("/image-test")
    public ResponseEntity<Resource> getImage(@RequestParam String imageName) {
        try {
            // C:/slam-uploads 폴더에서 직접 이미지를 찾아봅니다.
            Resource resource = new FileSystemResource(Paths.get(uploadDir, imageName));
            if (resource.exists() && resource.isReadable()) {
                // 파일을 찾았으면 이미지를 반환합니다.
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG) // 이미지 타입 설정
                        .body(resource);
            } else {
                // 파일을 못 찾았으면 404 에러를 반환합니다.
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            // 그 외 다른 에러가 발생하면 500 에러를 반환합니다.
            return ResponseEntity.status(500).build();
        }
    }
    // --- ⬆️ 여기까지 추가 ⬆️ ---

    // ✅ 추가: 이벤트 참석(RSVP) 등록 API
    @PostMapping("/rsvp")
    public ResponseEntity<?> submitRsvp(@RequestParam Long eventId, @RequestBody RsvpRequest rsvpRequest, Authentication authentication) {
        System.out.println("=== submitRsvp API Debug ===");
        System.out.println("Event ID: " + eventId);
        System.out.println("Authentication: " + (authentication != null ? authentication.getName() : "null"));
        System.out.println("RSVP Request - isAttending: " + rsvpRequest.isAttending() + ", afterParty: " + rsvpRequest.isAfterParty());
        System.out.println("RSVP Request toString: " + rsvpRequest.toString());
        
        if (authentication == null) {
            return ResponseEntity.status(401).body("Login required.");
        }

        try {
            String userEmail = authentication.getName();
            System.out.println("User Email: " + userEmail);
            
            EventRsvp savedRsvp = eventService.processRsvp(eventId, userEmail, rsvpRequest);
            
            System.out.println("Saved RSVP - ID: " + savedRsvp.getId() + 
                             ", Attending: " + savedRsvp.isAttending() + 
                             ", AfterParty: " + savedRsvp.isAfterParty());

            // 친구가 요청한 응답 형식
            return ResponseEntity.ok(Map.of("success", true, "message", "RSVP updated"));
        } catch (Exception e) {
            System.out.println("Error in submitRsvp: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ 추가: 내 RSVP 상태 조회 API
    @GetMapping("/my-rsvp")
    public ResponseEntity<?> getMyRsvp(@RequestParam Long eventId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Login required.");
        }

        try {
            String userEmail = authentication.getName();
            Optional<EventRsvp> rsvp = eventService.findMyRsvp(eventId, userEmail);

            if (rsvp.isPresent()) {
                EventRsvp rsvpData = rsvp.get();
                Map<String, Object> response = Map.of(
                    "attending", rsvpData.isAttending(),
                    "afterParty", rsvpData.isAfterParty(),
                    "createdAt", rsvpData.getCreatedAt()
                );
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(Map.of("attending", false, "afterParty", false));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ 이벤트 생성 API
    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody EventDTO eventDTO, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Login required.");
        }
        
        try {
            EventDTO savedEvent = eventService.createEvent(eventDTO);
            return ResponseEntity.ok(savedEvent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ 이벤트 수정 API
    @PutMapping("/{eventId}")
    public ResponseEntity<?> updateEvent(@PathVariable Long eventId, @RequestBody EventDTO eventDTO, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Login required.");
        }
        
        try {
            eventDTO = EventDTO.builder()
                    .id(eventId)
                    .branch(eventDTO.getBranch())
                    .title(eventDTO.getTitle())
                    .theme(eventDTO.getTheme())
                    .eventDateTime(eventDTO.getEventDateTime())
                    .location(eventDTO.getLocation())
                    .description(eventDTO.getDescription())
                    .imageUrl(eventDTO.getImageUrl())
                    .capacity(eventDTO.getCapacity())
                    .currentAttendees(eventDTO.getCurrentAttendees())
                    .price(eventDTO.getPrice())
                    .archived(eventDTO.isArchived())
                    .earlyBirdPrice(eventDTO.getEarlyBirdPrice())
                    .earlyBirdEndDate(eventDTO.getEarlyBirdEndDate())
                    .earlyBirdCapacity(eventDTO.getEarlyBirdCapacity())
                    .registrationDeadline(eventDTO.getRegistrationDeadline())
                    .capacityWarningThreshold(eventDTO.getCapacityWarningThreshold())
                    .showCapacityWarning(eventDTO.getShowCapacityWarning())
                    .endTime(eventDTO.getEndTime())
                    .bankAccount(eventDTO.getBankAccount())
                    .bankName(eventDTO.getBankName())
                    .accountName(eventDTO.getAccountName())
                    .eventType(eventDTO.getEventType())
                    .eventSequence(eventDTO.getEventSequence())
                    .productType(eventDTO.getProductType())
                    .build();
            
            EventDTO updatedEvent = eventService.updateEvent(eventId, eventDTO);
            return ResponseEntity.ok(updatedEvent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ 이벤트 삭제 API
    @DeleteMapping("/{eventId}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long eventId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Login required.");
        }
        
        try {
            eventService.deleteEvent(eventId);
            return ResponseEntity.ok(Map.of("message", "Event deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ 티켓 구매 API
    @PostMapping("/{eventId}/purchase-ticket")
    public ResponseEntity<?> purchaseTicket(
            @PathVariable Long eventId,
            @RequestBody Map<String, String> ticketInfo,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required."));
        }
        
        try {
            String userEmail = authentication.getName();
            
            // 이벤트 확인
            EventDTO eventDTO = eventService.findEventById(eventId);
            if (eventDTO == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "이벤트를 찾을 수 없습니다."));
            }
            
            // Special Event인지 확인
            if (!"SPECIAL_EVENT".equals(eventDTO.getEventType().toString())) {
                return ResponseEntity.badRequest().body(Map.of("error", "이 이벤트는 티켓 구매가 필요하지 않습니다."));
            }
            
            // ✅ 티켓 구매 시 멤버십 신청서만 생성 (승인 대기 상태)
            MembershipRequest membershipRequest = new MembershipRequest();
            membershipRequest.setSelectedBranch(eventDTO.getBranch());
            membershipRequest.setPaymentMethod("Ticket Purchase");
            membershipRequest.setUserType("Ticket Holder");
            
            // 티켓 구매 정보를 멤버십 신청서에 포함
            if (ticketInfo.containsKey("phone")) {
                membershipRequest.setPhone(ticketInfo.get("phone"));
            }
            if (ticketInfo.containsKey("specialRequests")) {
                membershipRequest.setFoodAllergies(ticketInfo.get("specialRequests"));
            }
            
            // ✅ 이벤트 ID를 포함하여 멤버십 신청
            membershipService.applyForMembershipWithEvent(userEmail, membershipRequest, eventId);
            
            return ResponseEntity.ok(Map.of(
                "message", "티켓 구매 신청이 완료되었습니다! Admin 승인을 기다려주세요.",
                "eventTitle", eventDTO.getTitle(),
                "ticketInfo", ticketInfo,
                "status", "PENDING_APPROVAL"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "티켓 구매 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
}