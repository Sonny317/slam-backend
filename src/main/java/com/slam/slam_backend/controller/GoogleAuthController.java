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
import com.slam.slam_backend.entity.UserStatus;
import com.slam.slam_backend.entity.MembershipType;
import java.util.HashMap;

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
            System.out.println("=== Google OAuth Callback Debug ===");
            System.out.println("Request body: " + request);
            
            String code = request.get("code");
            System.out.println("Authorization code: " + code);
            
            if (code == null || code.isEmpty()) {
                System.out.println("Error: Authorization code is missing");
                return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is required"));
            }
            
            // TODO: 실제 Google API 호출하여 액세스 토큰과 사용자 정보 가져오기
            // 현재는 임시 사용자 정보 (실제로는 Google API에서 가져와야 함)
            // 실제 구현에서는 Google API에서 받은 이메일을 사용해야 함
            String email = "google.user." + System.currentTimeMillis() + "@example.com"; // 임시 고유 이메일
            String name = "Google User"; // TODO: Google API에서 실제 이름 가져오기
            String providerId = "google_provider_id"; // TODO: Google API에서 실제 공급자 ID 가져오기
            
            System.out.println("Processing user: " + email);
            
            // 사용자가 존재하는지 확인 (provider_id로도 확인)
            User existingUser = userRepository.findByEmail(email).orElse(null);
            
            if (existingUser == null) {
                System.out.println("Creating new Google OAuth user: " + email);
                // 새 사용자 생성 (Google OAuth 사용자) - Builder 패턴 사용
                User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .password("") // Google OAuth 사용자는 비밀번호 없음
                    .role(UserRole.MEMBER) // 기본 역할 설정
                    .status(UserStatus.PRE_MEMBER) // 가회원 상태
                    .membershipType(MembershipType.NONE) // 기본 멤버십 타입
                    .profileImage(null)
                    .provider("google")
                    .providerId(providerId)
                    .oauthId(providerId)
                    .build();
                
                System.out.println("User object created with status: " + newUser.getStatus());
                
                existingUser = userRepository.save(newUser);
                System.out.println("New Google OAuth user created with ID: " + existingUser.getId());
            } else {
                System.out.println("Existing user found: " + existingUser.getId());
                // 기존 사용자가 Google OAuth 사용자가 아니라면 provider 정보 업데이트
                if (existingUser.getProvider() == null || !"google".equals(existingUser.getProvider())) {
                    existingUser.setProvider("google");
                    existingUser.setProviderId(providerId);
                    existingUser.setOauthId(providerId);
                    existingUser = userRepository.save(existingUser);
                    System.out.println("Updated existing user with Google OAuth info");
                }
            }
            
            // JWT 토큰 생성
            String token = jwtTokenProvider.generateToken(existingUser.getEmail());
            System.out.println("JWT token generated successfully");
            
            // null 값 처리를 위해 HashMap 사용
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Google OAuth 로그인이 성공했습니다.");
            response.put("code", code);
            response.put("status", "success");
            response.put("token", token);
            response.put("email", existingUser.getEmail());
            response.put("name", existingUser.getName() != null ? existingUser.getName() : "Unknown");
            response.put("profileImage", existingUser.getProfileImage());
            response.put("role", existingUser.getRole() != null ? existingUser.getRole().name() : "MEMBER");
            
            System.out.println("Response: " + response);
            System.out.println("=== Google OAuth Callback End ===");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("=== Google OAuth Callback Error ===");
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== Error End ===");
            
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
