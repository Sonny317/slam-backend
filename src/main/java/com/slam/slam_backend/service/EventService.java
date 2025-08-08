package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.EventDTO;
import com.slam.slam_backend.dto.EventRequest;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.HashMap;



@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventRsvpRepository eventRsvpRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public EventDTO findEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("해당 이벤트를 찾을 수 없습니다. id=" + eventId));
        return EventDTO.fromEntity(event);
    }

    @Transactional
    public EventRsvp processRsvp(Long eventId, String userEmail, RsvpRequest rsvpRequest) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + eventId));
        EventRsvp rsvp = eventRsvpRepository.findByUser_IdAndEvent_Id(user.getId(), eventId)
                .orElseGet(() -> EventRsvp.builder().user(user).event(event).build());
        rsvp.setAttending(rsvpRequest.isAttending());
        rsvp.setAfterParty(rsvpRequest.isAfterParty());
        return eventRsvpRepository.save(rsvp);
    }

    @Transactional(readOnly = true)
    public Optional<EventRsvp> findMyRsvp(Long eventId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));
        return eventRsvpRepository.findByUser_IdAndEvent_Id(user.getId(), eventId);
    }

    @Transactional
    public Event createEvent(EventRequest request) {
        Event newEvent = Event.builder()
                .branch(request.getBranch())
                .title(request.getTitle())
                .theme(request.getTheme())
                .eventDateTime(request.getEventDateTime())
                .location(request.getLocation())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .capacity(request.getCapacity())
                .price(request.getPrice())
                .currentAttendees(0)
                .build();
        return eventRepository.save(newEvent);
    }

    @Transactional
    public Event updateEvent(Long eventId, EventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + eventId));

        // Event 엔티티에 @Setter를 추가하거나, 필드를 직접 수정해야 합니다.
        // 여기서는 @Setter가 있다고 가정하고 진행합니다.
        event.setBranch(request.getBranch());
        event.setTitle(request.getTitle());
        event.setTheme(request.getTheme());
        event.setEventDateTime(request.getEventDateTime());
        event.setLocation(request.getLocation());
        event.setDescription(request.getDescription());
        event.setImageUrl(request.getImageUrl());
        event.setCapacity(request.getCapacity());
        event.setPrice(request.getPrice());

        return eventRepository.save(event);
    }



    // ✅ deleteEvent 메소드를 수정합니다.
    @Transactional
    public void deleteEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + eventId);
        }

        // 1. 이 이벤트에 연결된 모든 RSVP(티켓) 기록을 먼저 삭제합니다.
        eventRsvpRepository.deleteAllByEventId(eventId);

        // 2. 이제 이벤트를 안전하게 삭제할 수 있습니다.
        eventRepository.deleteById(eventId);
    }

    // ✅ 이벤트별 참석자 목록 조회
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEventAttendees(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + eventId));
        
        List<EventRsvp> rsvps = eventRsvpRepository.findByEvent_IdAndIsAttendingTrue(eventId);
        
        return rsvps.stream().map(rsvp -> {
            Map<String, Object> attendee = new HashMap<>();
            attendee.put("id", rsvp.getUser().getId());
            attendee.put("name", rsvp.getUser().getName());
            attendee.put("email", rsvp.getUser().getEmail());
            attendee.put("membership", rsvp.getUser().getMembership());
            attendee.put("afterParty", rsvp.isAfterParty());
            attendee.put("rsvpDate", rsvp.getCreatedAt());
            return attendee;
        }).collect(Collectors.toList());
    }

    // ✅ 이벤트별 참석 통계 조회
    @Transactional(readOnly = true)
    public Map<String, Object> getEventAttendanceStats(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + eventId));
        
        List<EventRsvp> allRsvps = eventRsvpRepository.findByEvent_Id(eventId);
        List<EventRsvp> attendingRsvps = eventRsvpRepository.findByEvent_IdAndIsAttendingTrue(eventId);
        List<EventRsvp> afterPartyRsvps = eventRsvpRepository.findByEvent_IdAndAfterPartyTrue(eventId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("eventId", eventId);
        stats.put("eventTitle", event.getTitle());
        stats.put("totalRsvps", allRsvps.size());
        stats.put("attendingCount", attendingRsvps.size());
        stats.put("notAttendingCount", allRsvps.size() - attendingRsvps.size());
        stats.put("afterPartyCount", afterPartyRsvps.size());
        stats.put("capacity", event.getCapacity());
        stats.put("attendanceRate", event.getCapacity() > 0 ? 
            (double) attendingRsvps.size() / event.getCapacity() * 100 : 0);
        
        return stats;
    }

    // ✅ QR 체크인 처리: 해당 사용자의 RSVP가 있으면 attended=true로 기록
    @Transactional
    public void markAttendance(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + eventId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        EventRsvp rsvp = eventRsvpRepository.findByUser_IdAndEvent_Id(userId, eventId)
                .orElseGet(() -> {
                    // RSVP가 없으면 기본 attending=true로 생성 후 체크인 처리
                    EventRsvp newRsvp = EventRsvp.builder()
                            .user(user)
                            .event(event)
                            .build();
                    newRsvp.setAttending(true);
                    newRsvp.setAfterParty(false);
                    newRsvp.setAttended(false);
                    return eventRsvpRepository.save(newRsvp);
                });

        rsvp.setAttended(true);
        eventRsvpRepository.save(rsvp);
    }
}