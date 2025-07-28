package com.slam.slam_backend.dto;

import com.slam.slam_backend.entity.MembershipApplication;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationDTO {
    private Long id;
    private String userName;
    private String userEmail;
    private String selectedBranch;
    private String paymentMethod;
    private String bankLast5;
    private String status;

    public static ApplicationDTO fromEntity(MembershipApplication entity) {
        return ApplicationDTO.builder()
                .id(entity.getId())
                .userName(entity.getUser().getName()) // 복잡한 User 객체 대신 이름만 추출
                .userEmail(entity.getUser().getEmail()) // 이메일만 추출
                .selectedBranch(entity.getSelectedBranch())
                .paymentMethod(entity.getPaymentMethod())
                .bankLast5(entity.getBankLast5())
                .status(entity.getStatus())
                .build();
    }
}