package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.LoginRequest;
import com.slam.slam_backend.dto.RegisterRequest;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.service.UserService;
import com.slam.slam_backend.entity.User;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

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

    @PostMapping("/upload-profile")
    public ResponseEntity<?> uploadProfileImage(@RequestParam("email") String email,
                                                @RequestParam("file") MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/images/";

        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        File destination = new File(uploadDir + filename);
        file.transferTo(destination); // ✅ 반드시 한 번만 호출!

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setProfileImage("/images/" + filename); // 🔁 프론트에서 접근할 수 있는 경로
        userRepository.save(user);

        return ResponseEntity.ok("프로필 업로드 완료");
    }



    // ✅ 토큰 JSON 형태로 반환할 내부 클래스
    static class TokenResponse {
        public String token;
        public TokenResponse(String token) {
            this.token = token;
        }
    }
}
