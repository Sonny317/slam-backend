package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.RegisterRequest;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final Map<String, String> verificationCodes = new ConcurrentHashMap<>();

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
                .affiliation(request.getAffiliation()) // ✅ 이 줄이 누락되어 있었습니다. 추가해주세요.
                .interests(request.getInterests())
                .spokenLanguages(request.getSpokenLanguages())
                .desiredLanguages(request.getDesiredLanguages())
                .role("MEMBER")
                .build();

        verificationCodes.remove(request.getEmail());
        return userRepository.save(user);
    }

    private String generateRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public User login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일이 존재하지 않습니다."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }
}