package com.slam.slam_backend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EventRequest {
    private String branch;
    private String title;
    private String theme;
    private LocalDateTime eventDateTime;
    private String location;
    private String description;
    private String imageUrl;
    private int capacity;
    private int price;
}