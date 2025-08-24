package com.slam.slam_backend.entity;

public enum MembershipType {
    NONE("No Membership"),
    FULL_SEMESTER("Full Semester", 1), // 1차부터 모든 이벤트
    MID_SEMESTER("Mid Semester", 2),   // 2차부터 이벤트
    LATE_SEMESTER("Late Semester", 3); // 3차부터 이벤트
    
    private final String displayName;
    private final int startEventSequence;
    
    MembershipType(String displayName) {
        this.displayName = displayName;
        this.startEventSequence = 0;
    }
    
    MembershipType(String displayName, int startEventSequence) {
        this.displayName = displayName;
        this.startEventSequence = startEventSequence;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getStartEventSequence() {
        return startEventSequence;
    }
    
    public boolean canJoinEvent(int eventSequence) {
        return this != NONE && eventSequence >= startEventSequence;
    }
}
