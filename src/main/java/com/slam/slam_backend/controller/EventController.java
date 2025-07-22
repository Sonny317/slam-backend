package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.EventDTO;
import com.slam.slam_backend.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // 이벤트 목록 조회 API (지부별 필터링 가능)
    @GetMapping
    public ResponseEntity<List<EventDTO>> getAllEvents(@RequestParam(required = false) String branch) {
        List<EventDTO> events = eventService.findAllEvents(branch);
        return ResponseEntity.ok(events);
    }

    // 이벤트 상세 정보 조회 API
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDTO> getEventById(@PathVariable Long eventId) {
        try {
            EventDTO event = eventService.findEventById(eventId);
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}