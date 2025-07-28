package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.*;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.security.JwtTokenProvider;
import com.slam.slam_backend.service.UserService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.slam.slam_backend.dto.PasswordResetRequest;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            userService.registerUser(request);
            return ResponseEntity.ok("회원가입 완료");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request.getEmail(), request.getPassword());
            String token = jwtTokenProvider.generateToken(user.getEmail());

            LoginResponse responseDto = LoginResponse.builder()
                    .token(token)
                    .email(user.getEmail())
                    .name(user.getName())
                    .profileImage(user.getProfileImage())
                    .build();

            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/upload-profile")
    public ResponseEntity<?> uploadProfileImage(@RequestParam("email") String email,
                                                @RequestParam("file") MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File destination = new File(uploadDir + File.separator + filename);
        file.transferTo(destination);

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        String imagePath = "/images/" + filename;
        user.setProfileImage(imagePath);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("profileImage", imagePath));
    }

    // ✅ 1. 자기소개 업데이트 전용 API
    @PostMapping("/api/users/profile/bio")
    public ResponseEntity<?> updateMyBio(Authentication authentication, @RequestBody Map<String, String> payload) {
        try {
            String userEmail = authentication.getName();
            String bio = payload.get("bio");
            User updatedUser = userService.updateBio(userEmail, bio);
            return ResponseEntity.ok(MyPageResponse.fromEntity(updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ 2. 프로필 이미지 업데이트 전용 API
    @PostMapping("/api/users/profile/image")
    public ResponseEntity<?> updateMyProfileImage(Authentication authentication, @RequestPart("file") MultipartFile file) {
        try {
            String userEmail = authentication.getName();
            User updatedUser = userService.updateProfileImage(userEmail, file);
            return ResponseEntity.ok(MyPageResponse.fromEntity(updatedUser));
        } catch (IOException e) {
            return ResponseEntity.status(500).body("프로필 이미지 업데이트 중 오류가 발생했습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            userService.sendVerificationCode(email);
            return ResponseEntity.ok("인증 코드가 이메일로 전송되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("이메일 전송에 실패했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/api/users/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("인증되지 않은 사용자입니다.");
        }
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        return ResponseEntity.ok(MyPageResponse.fromEntity(user));
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        try {
            userService.createPasswordResetTokenForUser(payload.get("email"));
            return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a password reset link has been sent."));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "If an account with that email exists, a password reset link has been sent."));
        }
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest request) {
        try {
            userService.changePassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password has been successfully reset."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


}