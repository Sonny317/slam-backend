package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 50)
    private String affiliation;

    @Column(length = 500)
    private String bio;

    @Column
    private String interests;

    @Column
    private String spokenLanguages;

    @Column
    private String desiredLanguages;

    @Column(length = 50)
    private String studentId;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String major;

    @Column(length = 50)
    private String nationality;

    @Column(length = 50)
    private String userType; // Local, International, Exchange 등

    @Column(length = 100)
    private String otherMajor;

    @Column(length = 100)
    private String professionalStatus;

    @Column(length = 50)
    private String country;

    @Column(length = 500)
    private String foodAllergies;

    @Column(length = 50)
    private String paymentMethod;

    @Column(length = 10)
    private String bankLast5;

    @Column(length = 100)
    private String industry;

    @Column(length = 100)
    private String networkingGoal;

    @Column(length = 200)
    private String otherNetworkingGoal;
}