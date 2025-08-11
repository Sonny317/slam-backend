package com.slam.slam_backend.dto;

import com.slam.slam_backend.entity.Event;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EventDTO {
    private Long id;
    private String branch;
    private String title;
    private String theme;
    private LocalDateTime eventDateTime;
    private String location;
    private String description;
    private String imageUrl;
    private int capacity;
    private int currentAttendees;
    private int price;
    private boolean archived;
    // TODO: 나중에 리뷰 목록도 여기에 추가할 수 있습니다.

    // Event 엔티티를 EventDTO로 변환하는 정적 메소드
    public static EventDTO fromEntity(Event event) {
        return EventDTO.builder()
                .id(event.getId())
                .branch(event.getBranch())
                .title(event.getTitle())
                .theme(event.getTheme())
                .eventDateTime(event.getEventDateTime())
                .location(event.getLocation())
                .description(event.getDescription())
                .imageUrl(event.getImageUrl())
                .capacity(event.getCapacity())
                .currentAttendees(event.getCurrentAttendees())
                .price(event.getPrice())
                .archived(event.isArchived())
                .build();
    }

    // EventDTO를 Event 엔티티로 변환하는 메소드
    public Event toEntity() {
        Event event = new Event();
        event.setId(this.id);
        event.setBranch(this.branch);
        event.setTitle(this.title);
        event.setTheme(this.theme);
        event.setEventDateTime(this.eventDateTime);
        event.setLocation(this.location);
        event.setDescription(this.description);
        event.setImageUrl(this.imageUrl);
        event.setCapacity(this.capacity);
        event.setCurrentAttendees(this.currentAttendees);
        event.setPrice(this.price);
        event.setArchived(this.archived);
        return event;
    }
}