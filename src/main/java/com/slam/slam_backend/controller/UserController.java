package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.LoginRequest;
import com.slam.slam_backend.dto.RegisterRequest;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.service.UserService;
import com.slam.slam_backend.entity.User;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping("/auth") // ✅ 경로를 /auth로 통일
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    // ✅ application.properties에 설정된 파일 업로드 경로를 주입받습니다.
    @Value("${file.upload-dir}")
    private String uploadDir;

    // ✅ 회원가입 처리 (RegisterRequest DTO를 받아서 처리)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            userService.registerUser(request);
            return ResponseEntity.ok("회원가입 완료");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = userService.loginAndGetToken(request.getEmail(), request.getPassword());
            return ResponseEntity.ok().body(new TokenResponse(token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ 프로필 업로드 로직 수정
    @PostMapping("/upload-profile")
    public ResponseEntity<?> uploadProfileImage(@RequestParam("email") String email,
                                                @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("업로드할 파일을 선택해주세요.");
        }

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // 설정된 외부 경로에 폴더가 없으면 생성합니다.
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 파일을 최종 목적지에 저장합니다.
        File destination = new File(uploadDir + File.separator + filename);
        file.transferTo(destination);

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        // 프론트엔드가 사용할 이미지 URL 경로
        String imagePath = "/images/" + filename;
        user.setProfileImage(imagePath);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("profileImage", imagePath));
    }

    // ✅ 6/24 추가: 자기소개 업데이트 API
    @PostMapping("/profile/update")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String bio = payload.get("bio");

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        user.setBio(bio);
        userRepository.save(user);

        return ResponseEntity.ok("Profile updated successfully");
    }



    // ✅ 토큰 JSON 형태로 반환할 내부 클래스
    static class TokenResponse {
        public String token;
        public TokenResponse(String token) {
            this.token = token;
        }
    }
}
