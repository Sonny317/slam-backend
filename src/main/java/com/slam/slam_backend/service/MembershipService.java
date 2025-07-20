package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.MembershipRequest;
import com.slam.slam_backend.entity.MembershipApplication;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.repository.MembershipApplicationRepository;
import com.slam.slam_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipApplicationRepository applicationRepository;
    private final UserRepository userRepository;

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
}