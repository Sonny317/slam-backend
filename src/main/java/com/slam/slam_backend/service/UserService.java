package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.RegisterRequest;
import com.slam.slam_backend.dto.UserUpdateRequest;
import com.slam.slam_backend.entity.PasswordResetToken;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.UserRole;
import com.slam.slam_backend.entity.UserStatus;
import com.slam.slam_backend.entity.VerificationCode;
import com.slam.slam_backend.repository.PasswordResetTokenRepository;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.regex.Pattern; // ✅ Pattern 임포트 추가

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository; // ✅ Repository 주입

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;
    
    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Transactional
    public void sendVerificationCode(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        String code = generateRandomCode();
        // 기존 레코드가 있으면 업데이트, 없으면 새로 생성하여 저장 (유니크 제약 위반 방지)
        verificationCodeRepository.findByEmail(email)
                .ifPresentOrElse(existing -> {
                    existing.setCode(code);
                    existing.setExpiryDate(LocalDateTime.now().plusMinutes(10));
                    verificationCodeRepository.save(existing);
                }, () -> {
                    verificationCodeRepository.save(new VerificationCode(email, code));
                });
        String subject = "[SLAM] Your verification code";
        String text = "To complete your registration, please enter the verification code below:\n\n" + "Verification code: " + code;
        emailService.sendEmail(email, subject, text);
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        VerificationCode storedCode = verificationCodeRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Verification code not issued or expired"));

        if (storedCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            verificationCodeRepository.delete(storedCode);
            throw new IllegalArgumentException("Verification code expired");
        }

        if (!storedCode.getCode().equals(request.getCode())) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        // --- ✅ 비밀번호 규칙 검증 로직 추가 ---
        validatePassword(request.getPassword());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .interests(request.getInterests())
                .spokenLanguages(request.getSpokenLanguages())
                .desiredLanguages(request.getDesiredLanguages())
                .role(UserRole.MEMBER)
                .status(UserStatus.PRE_MEMBER)
                .build();

        verificationCodeRepository.delete(storedCode);
        return userRepository.save(user);


    }

    // ✅ 비밀번호 규칙을 검사하는 private 메소드
    private void validatePassword(String password) {
        // 6자리 이상, 특수문자 포함
        String passwordRegex = "^(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{6,}$";
        if (!Pattern.matches(passwordRegex, password)) {
            throw new IllegalArgumentException("비밀번호는 특수문자를 포함하여 6자리 이상이어야 합니다.");
        }
    }

    public User login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일이 존재하지 않습니다."));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return user;
    }

    private String generateRandomCode() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));
    }

    // ✅ 1. 자기소개만 업데이트하는 메소드
    @Transactional
    public User updateBio(String email, String newBio) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));
        user.setBio(newBio);
        return userRepository.save(user);
    }

    // ✅ 2. 프로필 이미지만 업데이트하는 메소드
    @Transactional
    public User updateProfileImage(String email, MultipartFile newProfileImageFile) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        if (newProfileImageFile == null || newProfileImageFile.isEmpty()) {
            throw new IllegalArgumentException("업데이트할 이미지 파일이 없습니다.");
        }

        // 기존 이미지가 있다면 서버에서 파일 삭제
        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            Path oldImagePath = Paths.get(uploadDir, user.getProfileImage().replace("/images/", ""));
            Files.deleteIfExists(oldImagePath);
        }

        // 새 이미지 저장
        String filename = UUID.randomUUID() + "_" + newProfileImageFile.getOriginalFilename();
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File destination = new File(uploadDir + File.separator + filename);
        newProfileImageFile.transferTo(destination);

        user.setProfileImage("/images/" + filename);
        return userRepository.save(user);
    }

    // ✅ 추가: 비밀번호 재설정 요청 처리
    @Transactional
    public void createPasswordResetTokenForUser(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // 이메일이 존재하지 않아도, 보안을 위해 성공한 것처럼 보이게 아무 작업도 하지 않고 넘어갑니다.
            return;
        }
        String token = UUID.randomUUID().toString();
        PasswordResetToken myToken = new PasswordResetToken(user, token);
        passwordResetTokenRepository.save(myToken);

        // 프론트엔드의 비밀번호 재설정 페이지 주소 (환경 변수 기반)
        String resetUrl = frontendBaseUrl + "/reset-password?token=" + token;

        String subject = "[SLAM] Password reset request";
        String text = "Click the link below to reset your password:\n\n" + resetUrl;

        emailService.sendEmail(user.getEmail(), subject, text);
    }

    // ✅ 추가: 토큰을 이용한 비밀번호 변경
    @Transactional
    public void changePassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Token"));

        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("만료된 토큰입니다.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 사용 완료된 토큰은 삭제
        passwordResetTokenRepository.delete(resetToken);
    }

    // ✅ 마이페이지: 인증된 사용자의 비밀번호 변경
    @Transactional
    public void changePasswordForAuthenticatedUser(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

	    // ✅ 인증 코드 유효성 검증 메소드
	    public boolean verifyVerificationCode(String email, String code) {
	        return verificationCodeRepository.findByEmail(email)
	                .map(stored -> {
	                    if (stored.getExpiryDate().isBefore(LocalDateTime.now())) {
	                        // 만료된 코드는 정리
	                        verificationCodeRepository.delete(stored);
	                        return false;
	                    }
	                    return stored.getCode().equals(code);
	                })
	                .orElse(false);
	    }

	    // ✅ 사용자 기본 정보 업데이트 메소드
	    @Transactional
	    public User updateUserInfo(String email, UserUpdateRequest request) {
	        User user = userRepository.findByEmail(email)
	                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

	        // 이름 업데이트 (필수)
	        user.setName(request.getName());

	        // 선택적 정보 업데이트
	        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
	            user.setPhone(request.getPhone());
	        }

	        if (request.getMajor() != null && !request.getMajor().trim().isEmpty()) {
	            user.setMajor(request.getMajor());
	        }

	        if (request.getStudentId() != null && !request.getStudentId().trim().isEmpty()) {
	            user.setStudentId(request.getStudentId());
	        }

	        if (request.getBio() != null) {
	            user.setBio(request.getBio());
	        }

	        if (request.getInterests() != null) {
	            user.setInterests(request.getInterests());
	        }

	        if (request.getSpokenLanguages() != null) {
	            user.setSpokenLanguages(request.getSpokenLanguages());
	        }

	        if (request.getDesiredLanguages() != null) {
	            user.setDesiredLanguages(request.getDesiredLanguages());
	        }

	        return userRepository.save(user);
	    }
}