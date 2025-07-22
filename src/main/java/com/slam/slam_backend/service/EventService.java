package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.EventDTO;
import com.slam.slam_backend.entity.Event;
import com.slam.slam_backend.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;

    // 모든 이벤트 또는 특정 지부의 이벤트를 조회
    public List<EventDTO> findAllEvents(String branch) {
        List<Event> events;
        if (branch != null && !branch.isEmpty()) {
            events = eventRepository.findByBranch(branch);
        } else {
            events = eventRepository.findAll();
        }
        return events.stream()
                .map(EventDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // 특정 이벤트의 상세 정보를 ID로 조회
    public EventDTO findEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("해당 이벤트를 찾을 수 없습니다. id=" + eventId));
        return EventDTO.fromEntity(event);
    }
}