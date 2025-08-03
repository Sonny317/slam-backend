package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.EventDTO;
import com.slam.slam_backend.dto.RsvpRequest;
import com.slam.slam_backend.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; // ✅ 임포트 추가
import org.springframework.core.io.FileSystemResource; // ✅ 임포트 추가
import org.springframework.core.io.Resource; // ✅ 임포트 추가
import org.springframework.http.MediaType; // ✅ 임포트 추가
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // ✅ 임포트 추가
import com.slam.slam_backend.dto.RsvpRequest; // ✅ 임포트 추가
import com.slam.slam_backend.entity.EventRsvp; // ✅ 임포트 추가

import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths; // ✅ 임포트 추가
import java.util.List;
import java.util.Map;
import java.util.Optional; // ✅ 임포트 추가
import java.util.stream.Collectors; // ✅ Collectors 임포트 추가

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @Value("${file.upload-dir}") // ✅ application.properties에서 경로를 가져옵니다.
    private String uploadDir;

    @GetMapping
    public ResponseEntity<List<EventDTO>> getAllEvents(@RequestParam(required = false) String branch) {
        List<EventDTO> events = eventService.findAllEvents(branch);
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
        if (authentication == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        try {
            String userEmail = authentication.getName();
            eventService.processRsvp(eventId, userEmail, rsvpRequest);

            // 친구가 요청한 응답 형식
            return ResponseEntity.ok(Map.of("success", true, "message", "RSVP updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ 추가: 내 RSVP 상태 조회 API
    @GetMapping("/my-rsvp")
    public ResponseEntity<?> getMyRsvp(@RequestParam Long eventId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        String userEmail = authentication.getName();
        Optional<EventRsvp> rsvp = eventService.findMyRsvp(eventId, userEmail);

        // RSVP 정보가 있으면 그 정보를, 없으면 빈 객체를 반환
        return ResponseEntity.ok(rsvp.orElse(null));
    }
}