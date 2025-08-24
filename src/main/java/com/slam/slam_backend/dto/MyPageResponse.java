package com.slam.slam_backend.dto;

import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.UserMembership;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class MyPageResponse {
    private Long userId;
    private String name;
    private String email;
    private String bio;
    private String profileImage;
    private String role; // ✅ role 필드 추가
    private List<String> memberships; // ✅ String에서 List<String>으로 변경
    
    // ✅ 멤버십 관련 정보 (Single Source of Truth에서 가져옴)
    private String phone;
    private String studentId;
    private String major;
    private String nationality;
    private String userType;
    private String otherMajor;
    private String professionalStatus;
    private String country;
    private String foodAllergies;
    private String paymentMethod;
    private String bankLast5;
    private String industry;
    private String networkingGoal;
    private String otherNetworkingGoal;
    // 나중에 게시글, 댓글 목록도 여기에 추가할 수 있습니다.

    /// User 엔티티를 MyPageResponse DTO로 변환하는 정적 메소드
    public static MyPageResponse fromEntity(User user) {
        // ✅ 단일 멤버십 문자열을 리스트로 변환합니다.
        List<String> memberships = new ArrayList<>();
        
        // 디버그 로그 추가
        System.out.println("MyPageResponse - User ID: " + user.getId());
        System.out.println("MyPageResponse - User membership field: " + user.getMembership());
        System.out.println("MyPageResponse - User memberships collection size: " + user.getMemberships().size());
        
        // User.membership 필드가 null이거나 비어있으면 멤버십이 없는 것으로 간주
        if (user.getMembership() == null || user.getMembership().isEmpty()) {
            System.out.println("MyPageResponse - User has no membership field");
        } else {
            String membership = user.getMembership();
            System.out.println("MyPageResponse - Processing membership field: " + membership);
            
            if (membership.contains("_")) {
                // "ACTIVE_NCCU" 형태에서 지부 이름만 추출
                String[] parts = membership.split("_");
                if (parts.length >= 2) {
                    String status = parts[0];
                    String branchName = parts[1];
                    if ("ACTIVE".equals(status)) {
                        memberships.add(branchName);
                        System.out.println("MyPageResponse - Added from membership field: " + branchName);
                    }
                }
            } else {
                // 단순한 지부 이름인 경우 (예: "NTU", "NCCU")
                memberships.add(membership);
                System.out.println("MyPageResponse - Added from membership field: " + membership);
            }
        }
        
        // UserMembership 컬렉션이 비어있지 않은 경우에만 처리
        if (!user.getMemberships().isEmpty()) {
            List<String> activeMemberships = user.getMemberships().stream()
                    .filter(m -> m != null && "ACTIVE".equals(m.getStatus()) && m.getBranchName() != null && !m.getBranchName().isEmpty())
                    .map(UserMembership::getBranchName)
                    .collect(Collectors.toList());
            
            System.out.println("MyPageResponse - Active memberships from collection: " + activeMemberships);
            memberships.addAll(activeMemberships);
        } else {
            System.out.println("MyPageResponse - User has no memberships collection");
        }
        
                System.out.println("MyPageResponse - Final memberships: " + memberships);
        
        // 최종 검증: 멤버십이 실제로 유효한지 확인
        List<String> validMemberships = memberships.stream()
                .filter(membership -> membership != null && !membership.isEmpty() && !membership.equals("null"))
                .collect(Collectors.toList());
        
        System.out.println("MyPageResponse - Valid memberships after filtering: " + validMemberships);
        
        return MyPageResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .bio(user.getBio())
                .profileImage(user.getProfileImage())
                .role(user.getRole().name())
                .memberships(validMemberships)
                // ✅ 멤버십 관련 정보 포함 (Single Source of Truth)
                .phone(user.getPhone())
                .studentId(user.getStudentId())
                .major(user.getMajor())
                .nationality(user.getNationality())
                .userType(user.getUserType())
                .otherMajor(user.getOtherMajor())
                .professionalStatus(user.getProfessionalStatus())
                .country(user.getCountry())
                .foodAllergies(user.getFoodAllergies())
                .paymentMethod(user.getPaymentMethod())
                .bankLast5(user.getBankLast5())
                .industry(user.getIndustry())
                .networkingGoal(user.getNetworkingGoal())
                .otherNetworkingGoal(user.getOtherNetworkingGoal())
                .build();
    }
}
