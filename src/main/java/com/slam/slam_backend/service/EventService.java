package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.EventDTO;
import com.slam.slam_backend.dto.EventRequest;
import com.slam.slam_backend.dto.RsvpRequest;
import com.slam.slam_backend.entity.Event;
import com.slam.slam_backend.entity.EventRsvp;
import com.slam.slam_backend.entity.EventType;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.UserRole;
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
    public List<EventDTO> findAllEventsForUser(String branch, String userEmail) {
        List<Event> events;
        if (branch != null && !branch.isEmpty()) {
            events = eventRepository.findByBranch(branch);
        } else {
            events = eventRepository.findAll();
        }
        
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail).orElse(null);
        }
        
        final User finalUser = user;
        return events.stream()
                .map(event -> enrichEventForUser(event, finalUser))
                .collect(Collectors.toList());
    }
    
    private EventDTO enrichEventForUser(Event event, User user) {
        EventDTO dto = EventDTO.fromEntity(event);
        
        if (user == null) {
            // 비로그인 사용자
            dto = dto.toBuilder()
                    .canJoinForFree(false)
                    .joinButtonText("Join " + (event.getProductType() != null ? event.getProductType() : "Membership"))
                    .build();
        } else {
            // 로그인 사용자 권한 체크
            boolean canJoinForFree = canUserJoinForFree(user, event);
            String buttonText = canJoinForFree ? "Going/Not Going" : "Join " + (event.getProductType() != null ? event.getProductType() : "Membership");
            
            dto = dto.toBuilder()
                    .canJoinForFree(canJoinForFree)
                    .joinButtonText(buttonText)
                    .build();
        }
        
        return dto;
    }
    
    private boolean canUserJoinForFree(User user, Event event) {
        // ✅ Admin/Staff/President는 모든 이벤트에 무료 참석 가능
        if (user.getRole() == UserRole.ADMIN || 
            user.getRole() == UserRole.STAFF || 
            user.getRole() == UserRole.PRESIDENT || 
            user.getRole() == UserRole.LEADER) {
            return true;
        }
        
        // Special Event는 일반 사용자에게는 항상 결제 필요
        if (event.getEventType() == EventType.SPECIAL_EVENT) {
            return false;
        }
        
        // Regular Meet는 멤버십 타입에 따라 결정
        if (event.getEventType() == EventType.REGULAR_MEET && event.getEventSequence() != null) {
            return user.getMembershipType().canJoinEvent(event.getEventSequence());
        }
        
        return false;
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
                .orElseGet(() -> {
                    EventRsvp newRsvp = new EventRsvp();
                    newRsvp.setUser(user);
                    newRsvp.setEvent(event);
                    newRsvp.setAttended(false);
                    return newRsvp;
                });
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
    public EventDTO createEvent(EventDTO eventDTO) {
        Event newEvent = eventDTO.toEntity();
        newEvent.setCurrentAttendees(0);
        newEvent.setArchived(false);
        
        // ✅ Theme 기반으로 EventType과 ProductType 자동 설정
        autoSetEventTypeFromTheme(newEvent);
        
        Event savedEvent = eventRepository.save(newEvent);
        return EventDTO.fromEntity(savedEvent);
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

    @Transactional
    public Event createEvent(EventRequest request) {
        Event newEvent = new Event();
        newEvent.setBranch(request.getBranch());
        newEvent.setTitle(request.getTitle());
        newEvent.setTheme(request.getTheme());
        newEvent.setEventDateTime(request.getEventDateTime());
        newEvent.setLocation(request.getLocation());
        newEvent.setDescription(request.getDescription());
        newEvent.setImageUrl(request.getImageUrl());
        newEvent.setCapacity(request.getCapacity());
        newEvent.setPrice(request.getPrice());
        newEvent.setCurrentAttendees(0);
        newEvent.setArchived(false);
        return eventRepository.save(newEvent);
    }

    @Transactional
    public EventDTO updateEvent(Long eventId, EventDTO eventDTO) {
        Event existingEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + eventId));

        // ✅ 모든 새로운 필드들을 포함하여 업데이트
        existingEvent.setBranch(eventDTO.getBranch());
        existingEvent.setTitle(eventDTO.getTitle());
        existingEvent.setTheme(eventDTO.getTheme());
        existingEvent.setEventDateTime(eventDTO.getEventDateTime());
        existingEvent.setLocation(eventDTO.getLocation());
        existingEvent.setDescription(eventDTO.getDescription());
        existingEvent.setImageUrl(eventDTO.getImageUrl());
        existingEvent.setCapacity(eventDTO.getCapacity());
        existingEvent.setPrice(eventDTO.getPrice());
        existingEvent.setArchived(eventDTO.isArchived());
        
        // ✅ Early bird 관련 필드들
        existingEvent.setEarlyBirdPrice(eventDTO.getEarlyBirdPrice());
        existingEvent.setEarlyBirdEndDate(eventDTO.getEarlyBirdEndDate());
        existingEvent.setEarlyBirdCapacity(eventDTO.getEarlyBirdCapacity());
        
        // ✅ 데드라인 및 기타 새 필드들
        existingEvent.setRegistrationDeadline(eventDTO.getRegistrationDeadline());
        existingEvent.setCapacityWarningThreshold(eventDTO.getCapacityWarningThreshold());
        existingEvent.setShowCapacityWarning(eventDTO.getShowCapacityWarning());
        existingEvent.setEndTime(eventDTO.getEndTime());
        existingEvent.setBankAccount(eventDTO.getBankAccount());

        // ✅ Theme 변경 시 EventType과 ProductType 자동 재설정
        autoSetEventTypeFromTheme(existingEvent);

        Event savedEvent = eventRepository.save(existingEvent);
        return EventDTO.fromEntity(savedEvent);
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
        System.out.println("🔍 EventService.deleteEvent 시작 - Event ID: " + eventId);
        
        if (!eventRepository.existsById(eventId)) {
            System.out.println("❌ 이벤트를 찾을 수 없음 - Event ID: " + eventId);
            throw new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + eventId);
        }

        // 1. 이 이벤트에 연결된 모든 RSVP(티켓) 기록을 먼저 삭제합니다.
        System.out.println("🔄 RSVP 삭제 시작 - Event ID: " + eventId);
        long rsvpCount = eventRsvpRepository.findByEvent_Id(eventId).size();
        System.out.println("📊 삭제할 RSVP 개수: " + rsvpCount);
        
        eventRsvpRepository.deleteAllByEventId(eventId);
        System.out.println("✅ RSVP 삭제 완료 - Event ID: " + eventId);

        // 2. 이제 이벤트를 안전하게 삭제할 수 있습니다.
        System.out.println("🔄 이벤트 삭제 시작 - Event ID: " + eventId);
        eventRepository.deleteById(eventId);
        System.out.println("✅ 이벤트 삭제 완료 - Event ID: " + eventId);
        
        // 3. 삭제 후 확인
        boolean stillExists = eventRepository.existsById(eventId);
        System.out.println("🔍 삭제 후 존재 여부: " + stillExists);
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
                    EventRsvp newRsvp = new EventRsvp();
                    newRsvp.setUser(user);
                    newRsvp.setEvent(event);
                    newRsvp.setAttending(true);
                    newRsvp.setAfterParty(false);
                    newRsvp.setAttended(false);
                    return eventRsvpRepository.save(newRsvp);
                });

        rsvp.setAttended(true);
        eventRsvpRepository.save(rsvp);
    }
}