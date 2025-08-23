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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final UserRepository userRepository;
    private final StaffOnboardingTokenRepository staffOnboardingTokenRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    /**
     * 스태프 임명 시작 - 온보딩 토큰 생성 및 이메일 발송
     */
    @Transactional
    public void initiateStaffAssignment(String assignerEmail, StaffAssignmentRequest request) {
        // 임명자 권한 확인
        User assigner = userRepository.findByEmail(assignerEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Assigner not found: " + assignerEmail));

        UserRole assignerRole = assigner.getRole();
        if (!assignerRole.canAssignStaff()) {
            throw new IllegalArgumentException("You do not have permission to assign staff roles.");
        }

        // 계층 구조 기반 권한 검증
        UserRole targetRole = request.getTargetRole();
        if (!assignerRole.canAssignRole(targetRole)) {
            throw new IllegalArgumentException(
                String.format("%s does not have permission to assign %s role. (Hierarchy violation)", 
                            assignerRole.getDisplayName(), 
                            targetRole.getDisplayName())
            );
        }

        // 임명할 사용자 조회
        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Target user not found."));

        // 이미 해당 사용자의 미완료 온보딩이 있는지 확인
        Optional<StaffOnboardingToken> existingToken = staffOnboardingTokenRepository.findByUserAndCompleted(targetUser, false);
        if (existingToken.isPresent()) {
            StaffOnboardingToken existing = existingToken.get();
            
            // 3분 제한 체크
            if (!existing.canResendEmail()) {
                long remainingSeconds = existing.getSecondsUntilNextEmail();
                long remainingMinutes = remainingSeconds / 60;
                long remainingSecondsOnly = remainingSeconds % 60;
                throw new IllegalArgumentException(
                    String.format("Email can be resent after %d minutes %d seconds. (Spam prevention)", 
                                remainingMinutes, remainingSecondsOnly)
                );
            }
            
            // 3분이 지났으면 기존 토큰의 역할과 이메일 발송 시간 업데이트하고 재발송
            existing.setTargetRole(request.getTargetRole()); // 새로운 역할로 업데이트
            existing.updateLastEmailSent();
            staffOnboardingTokenRepository.save(existing);
            sendStaffOnboardingEmail(targetUser, existing.getToken(), existing.getTargetRole(), assigner);
            return; // 새 토큰 생성하지 않고 기존 토큰으로 재발송
        }

        // 온보딩 토큰 생성
        String token = UUID.randomUUID().toString();
        StaffOnboardingToken onboardingToken = StaffOnboardingToken.builder()
                .user(targetUser)
                .assignedBy(assigner)
                .targetRole(request.getTargetRole())
                .token(token)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .lastEmailSentAt(LocalDateTime.now()) // 초기 이메일 발송 시간 설정
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
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token."));

        User user = token.getUser();

        // 사용자 정보 업데이트
        updateUserWithStaffInfo(user, request);

        // ✅ President를 다른 사용자에게 임명하는 경우, 기존 President들을 Staff로 변경
        UserRole targetRole = token.getTargetRole();
        if (targetRole == UserRole.PRESIDENT) {
            List<User> existingPresidents = userRepository.findByRole(UserRole.PRESIDENT);
            for (User existingPresident : existingPresidents) {
                if (!existingPresident.getId().equals(user.getId())) {
                    existingPresident.setRole(UserRole.STAFF);
                    userRepository.save(existingPresident);
                    System.out.println("기존 President " + existingPresident.getName() + "를 Staff로 변경했습니다. (온보딩 프로세스)");
                }
            }
        }

        // 역할 변경
        user.setRole(targetRole);

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
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token."));
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
        
        String subject = "[SLAM] Congratulations! You have been appointed as staff.";
        String text = String.format(
                "Hello %s,\n\n" +
                "Congratulations! You have been appointed as SLAM %s by %s.\n\n" +
                "To start your staff activities, please click the link below to complete your profile information:\n\n" +
                "%s\n\n" +
                "This link is valid for 7 days.\n\n" +
                "Thank you.\n" +
                "SLAM Team",
                targetUser.getName(),
                targetRole.getDisplayName(),
                assigner.getName(),
                onboardingUrl
        );

        emailService.sendEmail(targetUser.getEmail(), subject, text);
        
        // Create notification for staff invitation
        createStaffInvitationNotification(targetUser, targetRole, assigner);
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

    /**
     * Staff 초청 알림 생성
     */
    private void createStaffInvitationNotification(User targetUser, UserRole targetRole, User assigner) {
        System.out.println("🔧 Staff 초청 알림 생성 - 사용자 이메일로 저장: " + targetUser.getEmail());
        
        notificationService.createStaffInvitationNotification(
            targetUser.getEmail(),  // ✅ 이메일로 변경 (ID 대신)
            assigner.getName(),
            targetRole.getDisplayName(),
            assigner.getId()
        );
    }
}
