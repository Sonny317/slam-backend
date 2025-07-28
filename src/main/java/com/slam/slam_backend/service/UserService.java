package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.RegisterRequest;
import com.slam.slam_backend.entity.PasswordResetToken;
import com.slam.slam_backend.entity.User;
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

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public void sendVerificationCode(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        String code = generateRandomCode();
        VerificationCode verificationCode = verificationCodeRepository.findByEmail(email)
                .orElse(new VerificationCode(email, code));
        verificationCodeRepository.save(new VerificationCode(email, code));
        String subject = "[SLAM] 회원가입 인증 코드입니다.";
        String text = "회원가입을 완료하려면 아래 인증 코드를 입력해주세요.\n\n" + "인증 코드: " + code;
        emailService.sendEmail(email, subject, text);
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        VerificationCode storedCode = verificationCodeRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("인증 코드가 발급되지 않았거나 만료되었습니다."));

        if (storedCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            verificationCodeRepository.delete(storedCode);
            throw new IllegalArgumentException("인증 코드가 만료되었습니다.");
        }

        if (!storedCode.getCode().equals(request.getCode())) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
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
                .role("MEMBER")
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

        // 프론트엔드의 비밀번호 재설정 페이지 주소
        String resetUrl = "http://localhost:3000/reset-password?token=" + token;

        String subject = "[SLAM] 비밀번호 재설정 요청";
        String text = "비밀번호를 재설정하려면 아래 링크를 클릭하세요:\n\n" + resetUrl;

        emailService.sendEmail(user.getEmail(), subject, text);
    }

    // ✅ 추가: 토큰을 이용한 비밀번호 변경
    @Transactional
    public void changePassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("만료된 토큰입니다.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 사용 완료된 토큰은 삭제
        passwordResetTokenRepository.delete(resetToken);
    }
}