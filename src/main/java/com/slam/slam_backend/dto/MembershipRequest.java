package com.slam.slam_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MembershipRequest {
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
}