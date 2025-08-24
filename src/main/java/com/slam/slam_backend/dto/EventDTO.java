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
    
    // ✅ 새로운 필드들
    private Integer earlyBirdPrice;
    private LocalDateTime earlyBirdEndDate;
    private Integer earlyBirdCapacity;
    private LocalDateTime registrationDeadline;
    private Integer capacityWarningThreshold;
    private Boolean showCapacityWarning;
    private String endTime;
    private String bankAccount;
    
    // ✅ 현재 가격 계산 (얼리버드 조건에 따라)
    private Integer currentPrice;
    private Boolean isEarlyBirdActive;
    
    // TODO: 나중에 리뷰 목록도 여기에 추가할 수 있습니다.

    // Event 엔티티를 EventDTO로 변환하는 정적 메소드
    public static EventDTO fromEntity(Event event) {
        // ✅ 얼리버드 가격 활성 여부 계산
        LocalDateTime now = LocalDateTime.now();
        boolean isEarlyBirdActive = false;
        Integer currentPrice = event.getPrice();
        
        if (event.getEarlyBirdPrice() != null) {
            boolean dateCondition = event.getEarlyBirdEndDate() == null || now.isBefore(event.getEarlyBirdEndDate());
            boolean capacityCondition = event.getEarlyBirdCapacity() == null || event.getCurrentAttendees() < event.getEarlyBirdCapacity();
            isEarlyBirdActive = dateCondition && capacityCondition;
            
            if (isEarlyBirdActive) {
                currentPrice = event.getEarlyBirdPrice();
            }
        }
        
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
                .earlyBirdPrice(event.getEarlyBirdPrice())
                .earlyBirdEndDate(event.getEarlyBirdEndDate())
                .earlyBirdCapacity(event.getEarlyBirdCapacity())
                .registrationDeadline(event.getRegistrationDeadline())
                .capacityWarningThreshold(event.getCapacityWarningThreshold())
                .showCapacityWarning(event.getShowCapacityWarning())
                .endTime(event.getEndTime())
                .bankAccount(event.getBankAccount())
                .currentPrice(currentPrice)
                .isEarlyBirdActive(isEarlyBirdActive)
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
        event.setEarlyBirdPrice(this.earlyBirdPrice);
        event.setEarlyBirdEndDate(this.earlyBirdEndDate);
        event.setEarlyBirdCapacity(this.earlyBirdCapacity);
        event.setRegistrationDeadline(this.registrationDeadline);
        event.setCapacityWarningThreshold(this.capacityWarningThreshold);
        event.setShowCapacityWarning(this.showCapacityWarning);
        event.setEndTime(this.endTime);
        event.setBankAccount(this.bankAccount);
        return event;
    }
}