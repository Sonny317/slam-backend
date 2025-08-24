package com.slam.slam_backend.entity;

public enum EventType {
    REGULAR_MEET("Regular SLAM Meet", "Membership"),
    SPECIAL_EVENT("Special Event", "Ticket");
    
    private final String displayName;
    private final String productType;
    
    EventType(String displayName, String productType) {
        this.displayName = displayName;
        this.productType = productType;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getProductType() {
        return productType;
    }
    
    public boolean isRegularMeet() {
        return this == REGULAR_MEET;
    }
    
    public boolean isSpecialEvent() {
        return this == SPECIAL_EVENT;
    }
}
