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

    @Transactional
    public void initiateStaffAssignment(String assignerEmail, StaffAssignmentRequest request) {
        User assigner = userRepository.findByEmail(assignerEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Assigner not found: " + assignerEmail));

        UserRole assignerRole = assigner.getRole();
        if (!assignerRole.canAssignStaff()) {
            throw new IllegalArgumentException("You do not have permission to assign staff roles.");
        }

        UserRole targetRole = request.getTargetRole();
        if (!assignerRole.canAssignRole(targetRole)) {
            throw new IllegalArgumentException(
                String.format("%s does not have permission to assign %s role. (Hierarchy violation)",
                            assignerRole.getDisplayName(),
                            targetRole.getDisplayName())
            );
        }

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Target user not found."));

        Optional<StaffOnboardingToken> existingToken = staffOnboardingTokenRepository.findByUserAndCompleted(targetUser, false);
        if (existingToken.isPresent()) {
            StaffOnboardingToken existing = existingToken.get();

            if (!existing.canResendEmail()) {
                long remainingSeconds = existing.getSecondsUntilNextEmail();
                long remainingMinutes = remainingSeconds / 60;
                long remainingSecondsOnly = remainingSeconds % 60;
                throw new IllegalArgumentException(
                    String.format("Email can be resent after %d minutes %d seconds. (Spam prevention)",
                                remainingMinutes, remainingSecondsOnly)
                );
            }

            existing.setTargetRole(request.getTargetRole());
            existing.updateLastEmailSent();
            staffOnboardingTokenRepository.save(existing);
            sendStaffOnboardingEmail(targetUser, existing.getToken(), existing.getTargetRole(), assigner);
            return;
        }

        String token = UUID.randomUUID().toString();
        StaffOnboardingToken onboardingToken = StaffOnboardingToken.builder()
                .user(targetUser)
                .assignedBy(assigner)
                .targetRole(request.getTargetRole())
                .token(token)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .lastEmailSentAt(LocalDateTime.now())
                .build();

        staffOnboardingTokenRepository.save(onboardingToken);
        sendStaffOnboardingEmail(targetUser, token, request.getTargetRole(), assigner);
    }

    @Transactional
    public void completeStaffOnboarding(StaffOnboardingRequest request) {
        StaffOnboardingToken token = staffOnboardingTokenRepository.findValidToken(
                request.getToken(), LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token."));

        User user = token.getUser();
        
        // --- 핵심 수정 부분 ---
        // 사용자 정보(프로필)를 UserProfile 엔티티에 업데이트합니다.
        updateUserProfileWithStaffInfo(user, request);

        if (token.getTargetRole() == UserRole.PRESIDENT) {
            List<User> existingPresidents = userRepository.findByRole(UserRole.PRESIDENT);
            for (User existingPresident : existingPresidents) {
                if (!existingPresident.getId().equals(user.getId())) {
                    existingPresident.setRole(UserRole.STAFF);
                    userRepository.save(existingPresident);
                }
            }
        }

        user.setRole(token.getTargetRole());
        if (user.getStatus() == UserStatus.PRE_MEMBER) {
            user.setStatus(UserStatus.ACTIVE_MEMBER);
        }

        userRepository.save(user);

        token.markAsCompleted();
        staffOnboardingTokenRepository.save(token);
    }

    public StaffOnboardingToken getOnboardingByToken(String token) {
        return staffOnboardingTokenRepository.findValidToken(token, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token."));
    }

    public List<StaffOnboardingToken> getPendingOnboardings() {
        return staffOnboardingTokenRepository.findByCompleted(false);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        List<StaffOnboardingToken> expiredTokens =
                staffOnboardingTokenRepository.findExpiredTokens(LocalDateTime.now());
        staffOnboardingTokenRepository.deleteAll(expiredTokens);
    }

    private void sendStaffOnboardingEmail(User targetUser, String token, UserRole targetRole, User assigner) {
        String onboardingUrl = frontendBaseUrl + "/staff-onboarding?token=" + token;
        
        String subject = "[SLAM] Congratulations! You have been appointed as staff.";
        String text = String.format(
                "Hello %s,\\n\\n" +
                "Congratulations! You have been appointed as SLAM %s by %s.\\n\\n" +
                "To start your staff activities, please click the link below to complete your profile information:\\n\\n" +
                "%s\\n\\n" +
                "This link is valid for 7 days.\\n\\n" +
                "Thank you.\\n" +
                "SLAM Team",
                targetUser.getName(),
                targetRole.getDisplayName(),
                assigner.getName(),
                onboardingUrl
        );

        emailService.sendEmail(targetUser.getEmail(), subject, text);
        createStaffInvitationNotification(targetUser, targetRole, assigner);
    }

    /**
     * 사용자 정보를 스태프 온보딩 데이터로 업데이트하는 메소드 (UserProfile 사용하도록 수정됨)
     */
    private void updateUserProfileWithStaffInfo(User user, StaffOnboardingRequest request) {
        UserProfile profile = user.getUserProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setUserProfile(profile);
        }

        if (request.getBio() != null && !request.getBio().trim().isEmpty()) {
            profile.setBio(request.getBio());
        }
        if (request.getStudentId() != null && !request.getStudentId().trim().isEmpty()) {
            profile.setStudentId(request.getStudentId());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            profile.setPhone(request.getPhoneNumber());
        }
        if (request.getMajor() != null && !request.getMajor().trim().isEmpty()) {
            profile.setMajor(request.getMajor());
        }
        if (request.getNationality() != null && !request.getNationality().trim().isEmpty()) {
            profile.setNationality(request.getNationality());
        }

        // 팀 정보를 affiliation 필드에 저장
        String teamInfo = String.format("%s (%s팀)", request.getUniversity(), request.getTeam());
        profile.setAffiliation(teamInfo);
    }

    private void createStaffInvitationNotification(User targetUser, UserRole targetRole, User assigner) {
        notificationService.createStaffInvitationNotification(
            targetUser.getEmail(),
            assigner.getName(),
            targetRole.getDisplayName(),
            assigner.getId()
        );
    }
}