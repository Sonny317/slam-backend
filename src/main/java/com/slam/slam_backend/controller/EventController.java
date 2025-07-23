package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.EventDTO;
import com.slam.slam_backend.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; // ✅ 임포트 추가
import org.springframework.core.io.FileSystemResource; // ✅ 임포트 추가
import org.springframework.core.io.Resource; // ✅ 임포트 추가
import org.springframework.http.MediaType; // ✅ 임포트 추가
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths; // ✅ 임포트 추가
import java.util.List;

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
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDTO> getEventById(@PathVariable Long eventId) {
        try {
            EventDTO event = eventService.findEventById(eventId);
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- ⬇️ 문제 진단을 위한 테스트 API ⬇️ ---
    @GetMapping("/image-test/{imageName}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageName) {
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
}