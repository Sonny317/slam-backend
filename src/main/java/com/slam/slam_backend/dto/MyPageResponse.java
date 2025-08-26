package com.slam.slam_backend.dto;

import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.UserMembership;
import com.slam.slam_backend.entity.UserProfile; // UserProfile 임포트
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
    private String role;
    private String profileImage;
    private List<String> memberships;

    // --- UserProfile에서 가져오는 정보 ---
    private String bio;
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
    private String interests;
    private String spokenLanguages;
    private String desiredLanguages;


    // User 엔티티를 MyPageResponse DTO로 변환하는 정적 메소드
    public static MyPageResponse fromEntity(User user) {
        // UserProfile이 null인 경우를 대비하여 빈 객체 생성
        UserProfile profile = user.getUserProfile() != null ? user.getUserProfile() : new UserProfile();

        List<String> activeMemberships = new ArrayList<>();
        if (user.getMemberships() != null && !user.getMemberships().isEmpty()) {
            activeMemberships = user.getMemberships().stream()
                    .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()))
                    .map(UserMembership::getBranchName)
                    .distinct()
                    .collect(Collectors.toList());
        }

        return MyPageResponse.builder()
                // --- User 엔티티에서 직접 가져오는 정보 ---
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .profileImage(user.getProfileImage())
                .memberships(activeMemberships)

                // --- UserProfile 엔티티에서 가져오는 정보 ---
                .bio(profile.getBio())
                .phone(profile.getPhone())
                .studentId(profile.getStudentId())
                .major(profile.getMajor())
                .nationality(profile.getNationality())
                .userType(profile.getUserType())
                .otherMajor(profile.getOtherMajor())
                .professionalStatus(profile.getProfessionalStatus())
                .country(profile.getCountry())
                .foodAllergies(profile.getFoodAllergies())
                .paymentMethod(profile.getPaymentMethod())
                .bankLast5(profile.getBankLast5())
                .industry(profile.getIndustry())
                .networkingGoal(profile.getNetworkingGoal())
                .otherNetworkingGoal(profile.getOtherNetworkingGoal())
                .interests(profile.getInterests())
                .spokenLanguages(profile.getSpokenLanguages())
                .desiredLanguages(profile.getDesiredLanguages())
                .build();
    }
}