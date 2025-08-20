package com.slam.slam_backend.entity;

/**
 * 사용자의 '상태'를 나타내는 enum
 * 사용자가 '무엇을 가지고 있는가'를 정의
 */
public enum UserStatus {
    /**
     * 가회원 - 이메일 회원가입만 마친 상태
     * 아직 유료 멤버십은 없음
     */
    PRE_MEMBER("Pre-member"),
    
    /**
     * 승인 대기 - 멤버십 비용을 냈다고 알렸지만
     * 아직 스태프가 확인하기 전 상태
     */
    PENDING_APPROVAL("Pending Approval"),
    
    /**
     * 정식 멤버 - 스태프가 입금을 확인하여 최종 승인한 유료 멤버
     */
    ACTIVE_MEMBER("Active Member");

    private final String displayName;

    UserStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
