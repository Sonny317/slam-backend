package com.slam.slam_backend.controller;

import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import com.slam.slam_backend.entity.UserRole;

@RestController
@RequiredArgsConstructor
public class GoogleAuthController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    @GetMapping("/api/auth/google/login")
    public ResponseEntity<?> googleLogin() {
        // Google OAuth 로그인 페이지로 리다이렉트
        String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=email profile" +
                "&access_type=offline" +
                "&prompt=consent";
        
        // 디버깅을 위한 로그
        System.out.println("=== Google OAuth Debug Info ===");
        System.out.println("Client ID: " + clientId);
        System.out.println("Redirect URI: " + redirectUri);
        System.out.println("Generated URL: " + googleAuthUrl);
        System.out.println("===============================");
        
        return ResponseEntity.ok(Map.of("authUrl", googleAuthUrl));
    }

    @PostMapping("/api/auth/google/callback")
    public ResponseEntity<?> googleCallback(@RequestBody Map<String, String> request) {
        try {
            String code = request.get("code");
            
            if (code == null || code.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is required"));
            }
            
            // TODO: 실제 Google API 호출하여 액세스 토큰과 사용자 정보 가져오기
            // 현재는 임시 사용자 정보 (실제로는 Google API에서 가져와야 함)
            String email = "google.user@example.com"; // TODO: Google API에서 실제 이메일 가져오기
            String name = "Google User"; // TODO: Google API에서 실제 이름 가져오기
            
            // 사용자가 존재하는지 확인
            User existingUser = userRepository.findByEmail(email).orElse(null);
            
            if (existingUser == null) {
                // 새 사용자 생성 (Google OAuth 사용자)
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setPassword(""); // Google OAuth 사용자는 비밀번호 없음
                newUser.setRole(UserRole.MEMBER); // 기본 역할 설정
                newUser.setProfileImage(null);
                
                existingUser = userRepository.save(newUser);
            }
            
            // JWT 토큰 생성
            String token = jwtTokenProvider.generateToken(existingUser.getEmail());
            
            return ResponseEntity.ok(Map.of(
                "message", "Google OAuth 로그인이 성공했습니다.",
                "code", code,
                "status", "success",
                "token", token,
                "email", existingUser.getEmail(),
                "name", existingUser.getName(),
                "profileImage", existingUser.getProfileImage(),
                "role", existingUser.getRole().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Google authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/api/auth/google/verify")
    public ResponseEntity<?> verifyGoogleToken(@RequestBody Map<String, String> request) {
        try {
            String googleToken = request.get("token");
            // TODO: Google 토큰 검증 로직 구현
            
            return ResponseEntity.ok(Map.of(
                "message", "Google 토큰 검증이 성공했습니다.",
                "status", "verified"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token verification failed: " + e.getMessage()));
        }
    }
}
