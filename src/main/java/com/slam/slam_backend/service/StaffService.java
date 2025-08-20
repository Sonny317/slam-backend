package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.StaffAssignmentRequest;
import com.slam.slam_backend.dto.StaffOnboardingRequest;
import com.slam.slam_backend.entity.*;
import com.slam.slam_backend.repository.StaffOnboardingTokenRepository;
import com.slam.slam_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final UserRepository userRepository;
    private final StaffOnboardingTokenRepository staffOnboardingTokenRepository;
    private final EmailService emailService;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    /**
     * 스태프 임명 시작 - 온보딩 토큰 생성 및 이메일 발송
     */
    @Transactional
    public void initiateStaffAssignment(String assignerEmail, StaffAssignmentRequest request) {
        // 임명자 권한 확인
        User assigner = userRepository.findByEmail(assignerEmail)
                .orElseThrow(() -> new UsernameNotFoundException("임명자를 찾을 수 없습니다: " + assignerEmail));

        if (!assigner.getRole().canAssignStaff()) {
            throw new IllegalArgumentException("스태프 임명 권한이 없습니다.");
        }

        // 임명할 사용자 조회
        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다."));

        // 이미 해당 사용자의 미완료 온보딩이 있는지 확인
        staffOnboardingTokenRepository.findByUserAndCompleted(targetUser, false)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("이미 진행 중인 스태프 온보딩이 있습니다.");
                });

        // 온보딩 토큰 생성
        String token = UUID.randomUUID().toString();
        StaffOnboardingToken onboardingToken = StaffOnboardingToken.builder()
                .user(targetUser)
                .assignedBy(assigner)
                .targetRole(request.getTargetRole())
                .token(token)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        staffOnboardingTokenRepository.save(onboardingToken);

        // 이메일 발송
        sendStaffOnboardingEmail(targetUser, token, request.getTargetRole(), assigner);
    }

    /**
     * 스태프 온보딩 완료 - 상세 정보 입력 처리
     */
    @Transactional
    public void completeStaffOnboarding(StaffOnboardingRequest request) {
        // 토큰 유효성 검증
        StaffOnboardingToken token = staffOnboardingTokenRepository.findValidToken(
                request.getToken(), LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다."));

        User user = token.getUser();

        // 사용자 정보 업데이트
        updateUserWithStaffInfo(user, request);

        // 역할 변경
        user.setRole(token.getTargetRole());

        // 상태 변경 (Pre-member에서 Active Member로)
        if (user.getStatus() == UserStatus.PRE_MEMBER) {
            user.setStatus(UserStatus.ACTIVE_MEMBER);
        }

        userRepository.save(user);

        // 온보딩 완료 표시
        token.markAsCompleted();
        staffOnboardingTokenRepository.save(token);
    }

    /**
     * 토큰으로 온보딩 정보 조회
     */
    public StaffOnboardingToken getOnboardingByToken(String token) {
        return staffOnboardingTokenRepository.findValidToken(token, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않거나 만료된 토큰입니다."));
    }

    /**
     * 모든 미완료 온보딩 조회 (관리자용)
     */
    public List<StaffOnboardingToken> getPendingOnboardings() {
        return staffOnboardingTokenRepository.findByCompleted(false);
    }

    /**
     * 만료된 토큰 정리
     */
    @Transactional
    public void cleanupExpiredTokens() {
        List<StaffOnboardingToken> expiredTokens = 
                staffOnboardingTokenRepository.findExpiredTokens(LocalDateTime.now());
        staffOnboardingTokenRepository.deleteAll(expiredTokens);
    }

    /**
     * 스태프 온보딩 이메일 발송
     */
    private void sendStaffOnboardingEmail(User targetUser, String token, UserRole targetRole, User assigner) {
        String onboardingUrl = frontendBaseUrl + "/staff-onboarding?token=" + token;
        
        String subject = "[SLAM] 축하합니다! 스태프로 임명되셨습니다.";
        String text = String.format(
                "안녕하세요 %s님,\n\n" +
                "축하드립니다! %s님에 의해 SLAM %s 스태프로 임명되셨습니다.\n\n" +
                "스태프 활동을 시작하기 위해 아래 링크를 클릭하여 상세 정보를 입력해주세요:\n\n" +
                "%s\n\n" +
                "이 링크는 7일간 유효합니다.\n\n" +
                "감사합니다.\n" +
                "SLAM 팀",
                targetUser.getName(),
                assigner.getName(),
                targetRole.getDisplayName(),
                onboardingUrl
        );

        emailService.sendEmail(targetUser.getEmail(), subject, text);
    }

    /**
     * 사용자 정보를 스태프 온보딩 데이터로 업데이트
     */
    private void updateUserWithStaffInfo(User user, StaffOnboardingRequest request) {
        // 기본 정보 업데이트
        if (request.getBio() != null && !request.getBio().trim().isEmpty()) {
            user.setBio(request.getBio());
        }
        
        if (request.getAffiliation() != null && !request.getAffiliation().trim().isEmpty()) {
            user.setAffiliation(request.getAffiliation());
        }

        // ✅ User 테이블의 새 필드들에 정보 저장
        if (request.getStudentId() != null && !request.getStudentId().trim().isEmpty()) {
            user.setStudentId(request.getStudentId());
        }
        
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            user.setPhone(request.getPhoneNumber());
        }
        
        if (request.getMajor() != null && !request.getMajor().trim().isEmpty()) {
            user.setMajor(request.getMajor());
        }

        // 팀 정보를 affiliation 필드에 저장
        String teamInfo = String.format("%s (%s팀)", request.getUniversity(), request.getTeam());
        user.setAffiliation(teamInfo);
    }
}
