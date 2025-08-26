package com.slam.slam_backend.dto;

import com.slam.slam_backend.entity.MembershipApplication;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ApplicationDTO {
    private Long id;
    private String userName;
    private String userEmail;
    private String selectedBranch;
    private String status;
    private LocalDateTime createdAt;
    
    // 프로필 정보는 UserProfile에서 가져옴
    private String userType;
    private String studentId;
    private String major;
    private String otherMajor;
    private String professionalStatus;
    private String country;
    private String phone;
    private String foodAllergies;
    private String paymentMethod;
    private String bankLast5;

    public static ApplicationDTO fromEntity(MembershipApplication entity) {
        // UserProfile에서 프로필 정보 가져오기
        var userProfile = entity.getUser().getUserProfile();
        
        return ApplicationDTO.builder()
                .id(entity.getId())
                .userName(entity.getUser().getName())
                .userEmail(entity.getUser().getEmail())
                .selectedBranch(entity.getSelectedBranch())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                // UserProfile에서 프로필 정보 가져오기 (null 체크)
                .userType(userProfile != null ? userProfile.getUserType() : null)
                .studentId(userProfile != null ? userProfile.getStudentId() : null)
                .major(userProfile != null ? userProfile.getMajor() : null)
                .otherMajor(userProfile != null ? userProfile.getOtherMajor() : null)
                .professionalStatus(userProfile != null ? userProfile.getProfessionalStatus() : null)
                .country(userProfile != null ? userProfile.getCountry() : null)
                .phone(userProfile != null ? userProfile.getPhone() : null)
                .foodAllergies(userProfile != null ? userProfile.getFoodAllergies() : null)
                .paymentMethod(userProfile != null ? userProfile.getPaymentMethod() : null)
                .bankLast5(userProfile != null ? userProfile.getBankLast5() : null)
                .build();
    }
}