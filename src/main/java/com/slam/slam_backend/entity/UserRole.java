package com.slam.slam_backend.entity;

/**
 * 사용자의 '역할'을 나타내는 enum
 * 사용자가 '무엇을 할 수 있는가'를 정의하는 권한
 */
public enum UserRole {
    /**
     * 일반 멤버
     */
    MEMBER("Member"),
    
    /**
     * 일반 스태프 (GA, PR, EP 등)
     */
    STAFF("Staff"),
    
    /**
     * 팀장급 스태프
     */
    LEADER("Leader"),
    
    /**
     * 각 지부의 회장
     * - 스태프 임명 권한 보유
     */
    PRESIDENT("President"),
    
    /**
     * 전체 시스템을 관리하는 최고 관리자
     * - 모든 권한 보유
     */
    ADMIN("Admin");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 스태프 임명 권한이 있는지 확인
     * @return PRESIDENT 또는 ADMIN인 경우 true
     */
    public boolean canAssignStaff() {
        return this == PRESIDENT || this == ADMIN;
    }

    /**
     * 관리자 권한이 있는지 확인
     * @return STAFF 이상의 권한을 가진 경우 true
     */
    public boolean hasAdminAccess() {
        return this == STAFF || this == LEADER || this == PRESIDENT || this == ADMIN;
    }
}
