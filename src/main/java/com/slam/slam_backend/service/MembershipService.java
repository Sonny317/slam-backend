package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.MembershipRequest;
import com.slam.slam_backend.entity.MembershipApplication;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.repository.MembershipApplicationRepository;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.entity.UserMembership; // ✅ 임포트 추가
import com.slam.slam_backend.repository.UserMembershipRepository; // ✅ 임포트 추가
import lombok.RequiredArgsConstructor;
import com.slam.slam_backend.dto.ApplicationDTO; // ✅ DTO 임포트
import lombok.Setter;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors; // ✅ Collectors 임포트

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final UserMembershipRepository userMembershipRepository;

    @Transactional
    public MembershipApplication applyForMembership(String userEmail, MembershipRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

        MembershipApplication application = MembershipApplication.builder()
                .user(user)
                .selectedBranch(request.getSelectedBranch())
                .userType(request.getUserType())
                .studentId(request.getStudentId())
                .major(request.getMajor())
                .otherMajor(request.getOtherMajor())
                .professionalStatus(request.getProfessionalStatus())
                .country(request.getCountry())
                .phone(request.getPhone())
                .foodAllergies(request.getFoodAllergies())
                .paymentMethod(request.getPaymentMethod())
                .bankLast5(request.getBankLast5())
                .status("payment_pending") // 초기 상태는 '결제 대기'
                .build();

        return applicationRepository.save(application);
    }

    // ✅ 추가: 모든 멤버십 신청 목록을 조회하는 메소드
    // ✅ 반환 타입을 List<ApplicationDTO>로 수정
    @Transactional(readOnly = true)
    public List<ApplicationDTO> findAllApplications() {
        return applicationRepository.findAll().stream()
                .map(ApplicationDTO::fromEntity) // ✅ Entity를 DTO로 변환
                .collect(Collectors.toList());
    }

    // ✅ 추가: 멤버십 신청을 승인하는 메소드
    @Transactional
    public void approveApplication(Long applicationId) {
        MembershipApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신청서를 찾을 수 없습니다: " + applicationId));

        // 1. 신청서의 상태를 'APPROVED'로 변경
        application.setStatus("APPROVED");
        applicationRepository.save(application);

        // 2. ✅ 새로운 UserMembership 객체를 생성하여 DB에 저장합니다.
        User user = application.getUser();
        UserMembership newMembership = UserMembership.builder()
                .user(user)
                .branchName(application.getSelectedBranch())
                .status("ACTIVE")
                .build();

        userMembershipRepository.save(newMembership);
    }

    // ✅ 추가: 멤버십 신청을 거부하는 메소드
    @Transactional
    public void rejectApplication(Long applicationId) {
        MembershipApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신청서를 찾을 수 없습니다: " + applicationId));

        // 신청서의 상태를 'REJECTED'로 변경
        application.setStatus("REJECTED");
        applicationRepository.save(application);
    }
}