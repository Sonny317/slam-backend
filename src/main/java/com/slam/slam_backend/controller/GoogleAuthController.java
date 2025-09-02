package com.slam.slam_backend.controller;

import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import com.slam.slam_backend.entity.UserRole;
import com.slam.slam_backend.entity.UserStatus;
import com.slam.slam_backend.entity.MembershipType;
import java.util.HashMap;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

@RestController
@RequiredArgsConstructor
public class GoogleAuthController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    @GetMapping("/api/auth/google/login")
    public ResponseEntity<?> googleLogin() {
        // Google OAuth Î°úÍ∑∏???òÏù¥ÏßÄÎ°?Î¶¨Îã§?¥Î†â??
        String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=email profile" +
                "&access_type=offline" +
                "&prompt=consent";
        
        // ?îÎ≤ÑÍπÖÏùÑ ?ÑÌïú Î°úÍ∑∏
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
            
            // 1. Authorization codeÎ°??°ÏÑ∏???†ÌÅ∞ ÍµêÌôò
            String tokenUrl = "https://oauth2.googleapis.com/token";
            HttpHeaders tokenHeaders = new HttpHeaders();
            tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            String tokenBody = String.format(
                "client_id=%s&client_secret=%s&code=%s&grant_type=authorization_code&redirect_uri=%s",
                clientId, clientSecret, code, redirectUri
            );
            
            HttpEntity<String> tokenRequest = new HttpEntity<>(tokenBody, tokenHeaders);
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUrl, tokenRequest, Map.class);
            
            if (tokenResponse.getStatusCode() != HttpStatus.OK || tokenResponse.getBody() == null) {
                System.out.println("Error: Failed to get access token");
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to get access token"));
            }
            
            String accessToken = (String) tokenResponse.getBody().get("access_token");
            System.out.println("Access token received: " + accessToken);
            
            // 2. ?°ÏÑ∏???†ÌÅ∞?ºÎ°ú ?¨Ïö©???ïÎ≥¥ Í∞Ä?∏Ïò§Í∏?
            String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.setBearerAuth(accessToken);
            
            HttpEntity<String> userInfoRequest = new HttpEntity<>(userInfoHeaders);
            ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                userInfoUrl, HttpMethod.GET, userInfoRequest, Map.class
            );
            
            if (userInfoResponse.getStatusCode() != HttpStatus.OK || userInfoResponse.getBody() == null) {
                System.out.println("Error: Failed to get user info");
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to get user info"));
            }
            
            Map<String, Object> userInfo = userInfoResponse.getBody();
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String providerId = (String) userInfo.get("id");
            String picture = (String) userInfo.get("picture");
            
            System.out.println("User info received - Email: " + email + ", Name: " + name + ", ID: " + providerId);
            
            if (email == null || email.isEmpty()) {
                System.out.println("Error: Email is required but not provided by Google");
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required but not provided by Google"));
            }
            
            // 3. ?¨Ïö©?êÍ? Ï°¥Ïû¨?òÎäîÏßÄ ?ïÏù∏ (?¥Î©î?ºÎ°ú ?ïÏù∏)
            User existingUser = userRepository.findByEmail(email).orElse(null);
            
            if (existingUser == null) {
                System.out.println("New user detected: " + email + " - Returning user data for terms agreement");
                // ?†Í∑ú ?¨Ïö©?êÏù∏ Í≤ΩÏö∞ ?¨Ïö©???∞Ïù¥??Î∞òÌôò (LoginPage?êÏÑú ?ΩÍ? ?ôÏùò Ï≤òÎ¶¨)
                Map<String, Object> userData = new HashMap<>();
                userData.put("email", email);
                userData.put("name", name != null ? name : "Google User");
                userData.put("providerId", providerId);
                userData.put("picture", picture);
                
                Map<String, Object> response = new HashMap<>();
                response.put("isNewUser", true);
                response.put("userData", userData);
                response.put("message", "New user - terms agreement required");
                response.put("status", "new_user");
                
                System.out.println("Response for new user: " + response);
                return ResponseEntity.ok(response);
            } else {
                System.out.println("Existing user found: " + existingUser.getId());
                // Í∏∞Ï°¥ ?¨Ïö©?êÍ? Google OAuth ?¨Ïö©?êÍ? ?ÑÎãà?ºÎ©¥ provider ?ïÎ≥¥ ?ÖÎç∞?¥Ìä∏
                if (existingUser.getProvider() == null || !"google".equals(existingUser.getProvider())) {
                    existingUser.setProvider("google");
                    existingUser.setProviderId(providerId);
                    existingUser.setOauthId(providerId);
                    if (picture != null) {
                        existingUser.setProfileImage(picture);
                    }
                    existingUser = userRepository.save(existingUser);
                    System.out.println("Updated existing user with Google OAuth info");
                }
                
                // Í∏∞Ï°¥ ?¨Ïö©?êÎäî Î∞îÎ°ú Î°úÍ∑∏??Ï≤òÎ¶¨
                String token = jwtTokenProvider.generateToken(existingUser.getEmail());
                System.out.println("JWT token generated successfully for existing user");
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Google OAuth Î°úÍ∑∏?∏Ïù¥ ?±Í≥µ?àÏäµ?àÎã§.");
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
            }
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
            // TODO: Google ?†ÌÅ∞ Í≤ÄÏ¶?Î°úÏßÅ Íµ¨ÌòÑ
            
            return ResponseEntity.ok(Map.of(
                "message", "Google ?†ÌÅ∞ Í≤ÄÏ¶ùÏù¥ ?±Í≥µ?àÏäµ?àÎã§.",
                "status", "verified"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token verification failed: " + e.getMessage()));
        }
    }
}
