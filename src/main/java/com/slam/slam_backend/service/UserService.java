// src/main/java/com/slam/slam_backend/service/UserService.java

package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.RegisterRequest;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public User registerUser(RegisterRequest request) {
        // ... (이메일 중복, 약관 동의 검사)

        // ✅ 실제 운영에서는 여기서 request.getCode()가 유효한지 검증해야 합니다.
        // 예를 들어, Redis나 별도 테이블에 저장된 인증코드와 일치하는지 확인하는 로직이 필요합니다.
        // if (!isVerificationCodeValid(request.getEmail(), request.getCode())) {
        //     throw new IllegalArgumentException("인증코드가 유효하지 않습니다.");
        // }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .affiliation(request.getAffiliation())
                .interests(request.getInterests())
                .spokenLanguages(request.getSpokenLanguages())
                .desiredLanguages(request.getDesiredLanguages())
                .role("USER")
                .build();

        return userRepository.save(user);
    }


    public String loginAndGetToken(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일이 존재하지 않습니다."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return jwtUtil.generateToken(user.getEmail());
    }
}