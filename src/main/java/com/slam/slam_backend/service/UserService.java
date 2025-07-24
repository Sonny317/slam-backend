package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.RegisterRequest;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.VerificationCode;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationCodeRepository verificationCodeRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    //private final Map<String, String> verificationCodes = new ConcurrentHashMap<>();

    @Transactional
    public void sendVerificationCode(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        String code = generateRandomCode();

        // 이메일로 기존 코드가 있는지 확인하고, 있다면 덮어쓰거나 새로 만듭니다.
        VerificationCode verificationCode = verificationCodeRepository.findByEmail(email)
                .orElse(new VerificationCode(email, code));

        verificationCodeRepository.save(new VerificationCode(email, code)); // ✅ DB에 저장

        String subject = "[SLAM] 회원가입 인증 코드입니다.";
        String text = "회원가입을 완료하려면 아래 인증 코드를 입력해주세요.\n\n" + "인증 코드: " + code;

        emailService.sendEmail(email, subject, text);
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        // ✅ DB에서 인증 코드를 가져와서 검증합니다.
        VerificationCode storedCode = verificationCodeRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("인증 코드가 발급되지 않았거나 만료되었습니다."));

        if (storedCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            verificationCodeRepository.delete(storedCode);
            throw new IllegalArgumentException("인증 코드가 만료되었습니다.");
        }

        if (!storedCode.getCode().equals(request.getCode())) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .interests(request.getInterests())
                .spokenLanguages(request.getSpokenLanguages())
                .desiredLanguages(request.getDesiredLanguages())
                .role("MEMBER")
                .build();

        verificationCodeRepository.delete(storedCode); // ✅ 검증 완료 후 DB에서 코드 삭제
        return userRepository.save(user);
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

    // ✅ 프로필 정보 업데이트를 위한 새로운 통합 메소드
    @Transactional
    public User updateUserProfile(String email, String newBio, MultipartFile newProfileImageFile) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        // 1. 자기소개 업데이트 (요청에 bio가 포함된 경우)
        if (newBio != null) {
            user.setBio(newBio);
        }

        // 2. 프로필 이미지 업데이트 (요청에 파일이 포함된 경우)
        if (newProfileImageFile != null && !newProfileImageFile.isEmpty()) {
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
        }

        return userRepository.save(user);
    }
}