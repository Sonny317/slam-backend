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
     * @return LEADER 이상의 권한을 가진 경우 true
     */
    public boolean canAssignStaff() {
        return this == LEADER || this == PRESIDENT || this == ADMIN;
    }

    /**
     * 관리자 권한이 있는지 확인
     * @return STAFF 이상의 권한을 가진 경우 true
     */
    public boolean hasAdminAccess() {
        return this == STAFF || this == LEADER || this == PRESIDENT || this == ADMIN;
    }

    /**
     * 특정 역할을 임명할 수 있는지 확인 (계층 구조 기반)
     * @param targetRole 임명하려는 역할
     * @return 임명 가능하면 true
     */
    public boolean canAssignRole(UserRole targetRole) {
        switch (this) {
            case ADMIN:
                // Admin은 모든 역할 임명 가능 (Admin 제외)
                return targetRole != ADMIN;
            case PRESIDENT:
                // President는 하위 역할만 임명 가능 (Admin, President 제외)
                return targetRole == LEADER || targetRole == STAFF || targetRole == MEMBER;
            case LEADER:
                // Leader는 Staff와 Member만 임명 가능
                return targetRole == STAFF || targetRole == MEMBER;
            case STAFF:
            case MEMBER:
                // Staff와 Member는 임명 권한 없음
                return false;
            default:
                return false;
        }
    }

    /**
     * 역할의 계층 레벨 반환 (숫자가 낮을수록 높은 권한)
     * @return 계층 레벨
     */
    public int getHierarchyLevel() {
        switch (this) {
            case ADMIN: return 1;
            case PRESIDENT: return 2;
            case LEADER: return 3;
            case STAFF: return 4;
            case MEMBER: return 5;
            default: return 99;
        }
    }

    /**
     * 다른 역할보다 높은 권한인지 확인
     * @param other 비교할 역할
     * @return 더 높은 권한이면 true
     */
    public boolean isHigherThan(UserRole other) {
        return this.getHierarchyLevel() < other.getHierarchyLevel();
    }
}
