package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.MembershipRequest;
import com.slam.slam_backend.entity.MembershipApplication;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.UserStatus;
import com.slam.slam_backend.repository.MembershipApplicationRepository;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.entity.UserMembership; // ✅ 임포트 추가
import com.slam.slam_backend.repository.UserMembershipRepository; // ✅ 임포트 추가
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import com.slam.slam_backend.dto.ApplicationDTO; // ✅ DTO 임포트
import lombok.Setter;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors; // ✅ Collectors 임포트

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final UserMembershipRepository userMembershipRepository;
    
    @Autowired
    private NotificationService notificationService;

    @Transactional
    public MembershipApplication applyForMembership(String userEmail, MembershipRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

        // ✅ Single Source of Truth: User 테이블을 직접 업데이트
        if (request.getUserType() != null && !request.getUserType().trim().isEmpty()) {
            user.setUserType(request.getUserType());
        }
        if (request.getStudentId() != null && !request.getStudentId().trim().isEmpty()) {
            user.setStudentId(request.getStudentId());
        }
        if (request.getMajor() != null && !request.getMajor().trim().isEmpty()) {
            user.setMajor(request.getMajor());
        }
        if (request.getOtherMajor() != null && !request.getOtherMajor().trim().isEmpty()) {
            user.setOtherMajor(request.getOtherMajor());
        }
        if (request.getProfessionalStatus() != null && !request.getProfessionalStatus().trim().isEmpty()) {
            user.setProfessionalStatus(request.getProfessionalStatus());
        }
        if (request.getCountry() != null && !request.getCountry().trim().isEmpty()) {
            user.setCountry(request.getCountry());
            user.setNationality(request.getCountry()); // nationality도 동기화
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone());
        }
        if (request.getFoodAllergies() != null && !request.getFoodAllergies().trim().isEmpty()) {
            user.setFoodAllergies(request.getFoodAllergies());
        }
        if (request.getPaymentMethod() != null && !request.getPaymentMethod().trim().isEmpty()) {
            user.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getBankLast5() != null && !request.getBankLast5().trim().isEmpty()) {
            user.setBankLast5(request.getBankLast5());
        }

        // User 테이블 업데이트
        userRepository.save(user);

        // ✅ 신청서는 이제 참조용/히스토리용으로만 저장
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

    @Transactional(readOnly = true)
    public MembershipApplication findMyLatestApplication(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));
        return applicationRepository.findTopByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void updateProfileFromMembership(String userEmail, com.slam.slam_backend.dto.MembershipRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

        // ✅ 모든 멤버십 관련 필드를 User 테이블에 업데이트
        if (request.getUserType() != null && !request.getUserType().trim().isEmpty()) {
            user.setUserType(request.getUserType());
        }
        if (request.getStudentId() != null && !request.getStudentId().trim().isEmpty()) {
            user.setStudentId(request.getStudentId());
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone());
        }
        if (request.getMajor() != null && !request.getMajor().trim().isEmpty()) {
            user.setMajor(request.getMajor());
        }
        if (request.getOtherMajor() != null && !request.getOtherMajor().trim().isEmpty()) {
            user.setOtherMajor(request.getOtherMajor());
        }
        if (request.getProfessionalStatus() != null && !request.getProfessionalStatus().trim().isEmpty()) {
            user.setProfessionalStatus(request.getProfessionalStatus());
        }
        if (request.getCountry() != null && !request.getCountry().trim().isEmpty()) {
            user.setCountry(request.getCountry());
            user.setNationality(request.getCountry());
        }
        if (request.getFoodAllergies() != null && !request.getFoodAllergies().trim().isEmpty()) {
            user.setFoodAllergies(request.getFoodAllergies());
        }
        if (request.getPaymentMethod() != null && !request.getPaymentMethod().trim().isEmpty()) {
            user.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getBankLast5() != null && !request.getBankLast5().trim().isEmpty()) {
            user.setBankLast5(request.getBankLast5());
        }

        userRepository.save(user);
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

        // 3. ✅ 사용자 엔티티의 membership 필드도 동기화 (단일 표기용)
        user.setMembership(application.getSelectedBranch());
        
        // 4. ✅ 신청서의 상세 정보를 User 테이블에도 저장
        if (application.getStudentId() != null && !application.getStudentId().trim().isEmpty()) {
            user.setStudentId(application.getStudentId());
        }
        if (application.getPhone() != null && !application.getPhone().trim().isEmpty()) {
            user.setPhone(application.getPhone());
        }
        if (application.getMajor() != null && !application.getMajor().trim().isEmpty()) {
            user.setMajor(application.getMajor());
        }
        
        // 5. ✅ 사용자 상태를 ACTIVE_MEMBER로 변경
        user.setStatus(UserStatus.ACTIVE_MEMBER);
        
        userRepository.save(user);
        
        // 6. 승인 알림 생성
        notificationService.createMembershipNotification(user.getEmail(), application.getSelectedBranch(), true);
    }

    // ✅ 추가: 멤버십 신청을 거부하는 메소드
    @Transactional
    public void rejectApplication(Long applicationId) {
        MembershipApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신청서를 찾을 수 없습니다: " + applicationId));

        // 신청서의 상태를 'REJECTED'로 변경
        application.setStatus("REJECTED");
        applicationRepository.save(application);
        
        // 거절 알림 생성
        User user = application.getUser();
        notificationService.createMembershipNotification(user.getEmail(), application.getSelectedBranch(), false);
    }

    // ✅ 추가: 멤버십 가격 정보를 제공하는 메소드
    @Transactional(readOnly = true)
    public Map<String, Object> getMembershipPricing(String branch) {
        Map<String, Object> pricing = new HashMap<>();
        
        // 현재 멤버 수 조회
        long currentMembers = userMembershipRepository.countByBranchNameIgnoreCaseAndStatusIgnoreCase(branch, "ACTIVE");
        
        // 기본 가격 설정 (얼리버드 기준: 20명, 얼리버드 가격: 800, 정가: 900)
        int earlyBirdCap = 20;
        int earlyBirdPrice = 800;
        int regularPrice = 900;
        
        // 현재 가격 결정
        int currentPrice = currentMembers < earlyBirdCap ? earlyBirdPrice : regularPrice;
        
        pricing.put("currentMembers", currentMembers);
        pricing.put("earlyBirdCap", earlyBirdCap);
        pricing.put("earlyBirdPrice", earlyBirdPrice);
        pricing.put("regularPrice", regularPrice);
        pricing.put("currentPrice", currentPrice);
        pricing.put("isEarlyBirdActive", currentMembers < earlyBirdCap);
        pricing.put("totalCapacity", 80); // 총 정원
        
        return pricing;
    }
}