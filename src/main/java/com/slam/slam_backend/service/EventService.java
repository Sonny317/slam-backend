package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.EventDTO;
import com.slam.slam_backend.dto.RsvpRequest;
import com.slam.slam_backend.entity.Event;
import com.slam.slam_backend.entity.EventRsvp;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.repository.EventRepository;
import com.slam.slam_backend.repository.EventRsvpRepository;
import com.slam.slam_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional; // ✅ Optional 임포트 추가

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {
    private final EventRsvpRepository eventRsvpRepository; // ✅ Repository 주입
    private final UserRepository userRepository; // ✅ Repository 주입
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

    // ✅ 추가: 이벤트 참석(RSVP) 처리 메소드
    @Transactional
    public EventRsvp processRsvp(Long eventId, String userEmail, RsvpRequest rsvpRequest) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + eventId));

        // 사용자가 이 이벤트에 대해 이미 RSVP를 했는지 확인
        EventRsvp rsvp = eventRsvpRepository.findByUser_IdAndEvent_Id(user.getId(), eventId)
                .orElse(EventRsvp.builder().user(user).event(event).build()); // 없다면 새로 생성

        // 새로운 정보로 업데이트
        EventRsvp updatedRsvp = EventRsvp.builder()
                .id(rsvp.getId()) // 기존 ID 유지
                .user(user)
                .event(event)
                .isAttending(rsvpRequest.isAttending())
                .afterParty(rsvpRequest.isAfterParty())
                .build();

        return eventRsvpRepository.save(updatedRsvp);
    }

    // ✅ 추가: 내 RSVP 상태를 조회하는 메소드
    @Transactional(readOnly = true)
    public Optional<EventRsvp> findMyRsvp(Long eventId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

        return eventRsvpRepository.findByUser_IdAndEvent_Id(user.getId(), eventId);
    }
}