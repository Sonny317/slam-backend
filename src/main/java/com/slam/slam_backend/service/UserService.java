package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.RegisterRequest;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.repository.UserRepository;
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

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final Map<String, String> verificationCodes = new ConcurrentHashMap<>();

    // ... (sendVerificationCode, registerUser, login, generateRandomCode 메소드는 기존과 동일)
    public void sendVerificationCode(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        String code = generateRandomCode();
        verificationCodes.put(email, code);
        String subject = "[SLAM] 회원가입 인증 코드입니다.";
        String text = "회원가입을 완료하려면 아래 인증 코드를 입력해주세요.\n\n" + "인증 코드: " + code;
        emailService.sendEmail(email, subject, text);
    }

    public User registerUser(RegisterRequest request) {
        String storedCode = verificationCodes.get(request.getEmail());
        if (storedCode == null || !storedCode.equals(request.getCode())) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))

                .role("MEMBER")
                .build();
        verificationCodes.remove(request.getEmail());
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