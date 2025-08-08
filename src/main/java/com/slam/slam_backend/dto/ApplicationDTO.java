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
    private String status;
    private LocalDateTime createdAt;

    public static ApplicationDTO fromEntity(MembershipApplication entity) {
        return ApplicationDTO.builder()
                .id(entity.getId())
                .userName(entity.getUser().getName()) // 복잡한 User 객체 대신 이름만 추출
                .userEmail(entity.getUser().getEmail()) // 이메일만 추출
                .selectedBranch(entity.getSelectedBranch())
                .userType(entity.getUserType())
                .studentId(entity.getStudentId())
                .major(entity.getMajor())
                .otherMajor(entity.getOtherMajor())
                .professionalStatus(entity.getProfessionalStatus())
                .country(entity.getCountry())
                .phone(entity.getPhone())
                .foodAllergies(entity.getFoodAllergies())
                .paymentMethod(entity.getPaymentMethod())
                .bankLast5(entity.getBankLast5())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}