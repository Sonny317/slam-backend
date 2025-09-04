package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.*;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.repository.PostRepository;
import com.slam.slam_backend.repository.CommentRepository;
import com.slam.slam_backend.security.JwtTokenProvider;
import com.slam.slam_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.slam.slam_backend.dto.PasswordResetRequest;
import com.slam.slam_backend.entity.UserStatus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            System.out.println("=== Controller Register Debug ===");
            System.out.println("Request body isGoogleUser: " + request.isGoogleUser());
            System.out.println("Request body email: " + request.getEmail());
            System.out.println("Request body name: " + request.getName());
            System.out.println("Request body googleId: " + request.getGoogleId());
            System.out.println("Request body terms: " + request.isTermsOfServiceAgreed() + ", " + request.isPrivacyPolicyAgreed() + ", " + request.isEventPhotoAgreed());
            
            User user = userService.registerUser(request);
            System.out.println("User registered successfully: " + user.getEmail());
            
            // Google OAuth 사용자인 경우 바로 토큰 생성하여 반환
            if (request.isGoogleUser()) {
                String token = jwtTokenProvider.generateToken(user.getEmail());
                System.out.println("JWT token generated for Google user: " + user.getEmail());
                
                LoginResponse responseDto = LoginResponse.builder()
                        .token(token)
                        .email(user.getEmail())
                        .name(user.getName())
                        .profileImage(user.getProfileImage())
                        .role(user.getRole().name())
                        .build();
                
                System.out.println("Returning response for Google user registration");
                return ResponseEntity.ok(responseDto);
            } else {
                // 일반 회원가입의 경우 기존과 동일
                return ResponseEntity.ok("Registration completed");
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Registration error: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected registration error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
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
                    .role(user.getRole().name()) // ✅ enum을 String으로 변환
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
            return ResponseEntity.status(500).body("An error occurred while updating the profile image.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            userService.sendVerificationCode(email);
            return ResponseEntity.ok("Verification code has been sent to your email.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send email: " + e.getMessage());
        }
    }

    // ✅ 인증코드 검증 API (프론트의 Verify 버튼에서 호출)
    @PostMapping("/auth/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String code = payload.get("code");
        if (email == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Missing email or code"));
        }
        boolean valid = userService.verifyVerificationCode(email, code);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    // ✅ 이메일 중복 확인 API
    @GetMapping("/api/auth/check-email")
    public ResponseEntity<?> checkEmailDuplicate(@RequestParam("email") String email) {
        boolean exists = userRepository.findByEmail(email).isPresent();
        return ResponseEntity.ok(Map.of("available", !exists));
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

    // ✅ 마이페이지: 비밀번호 변경 API
    @PostMapping("/api/users/change-password")
    public ResponseEntity<?> changeMyPassword(Authentication authentication, @RequestBody ChangePasswordRequest request) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            String userEmail = authentication.getName();
            userService.changePasswordForAuthenticatedUser(userEmail, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to change password"));
        }
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

    // 사용자별 게시글 조회
    @GetMapping("/api/users/me/posts")
    public ResponseEntity<?> getMyPosts(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("인증되지 않은 사용자입니다.");
        }
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 사용자 이름과 이메일 모두로 게시글 검색
        List<PostDTO> postsByEmail = postRepository.findByAuthorOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(PostDTO::new)
                .collect(Collectors.toList());

        List<PostDTO> postsByName = postRepository.findByAuthorOrderByCreatedAtDesc(user.getName())
                .stream()
                .map(PostDTO::new)
                .collect(Collectors.toList());

        // 두 결과를 합치고 중복 제거
        List<PostDTO> allPosts = new ArrayList<>();
        allPosts.addAll(postsByEmail);
        allPosts.addAll(postsByName);
        
        // ID 기준으로 중복 제거
        List<PostDTO> uniquePosts = allPosts.stream()
                .collect(Collectors.toMap(
                    PostDTO::getId,
                    post -> post,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(uniquePosts);
    }

    // ✅ 특정 사용자 프로필 조회 (이름, 사진, 작성글 목록 등)
    @GetMapping("/api/users/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 작성글 목록
        List<PostDTO> postsByEmail = postRepository.findByAuthorOrderByCreatedAtDesc(user.getEmail())
                .stream()
                .map(PostDTO::new)
                .collect(Collectors.toList());
        List<PostDTO> postsByName = postRepository.findByAuthorOrderByCreatedAtDesc(user.getName())
                .stream()
                .map(PostDTO::new)
                .collect(Collectors.toList());
        List<PostDTO> allPosts = new java.util.ArrayList<>();
        allPosts.addAll(postsByEmail);
        allPosts.addAll(postsByName);
        List<PostDTO> uniquePosts = allPosts.stream()
                .collect(Collectors.toMap(
                        PostDTO::getId,
                        post -> post,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .collect(Collectors.toList());

        // 댓글 목록
        List<CommentDTO> commentsByEmail = commentRepository.findByAuthorOrderByCreatedAtDesc(user.getEmail())
                .stream()
                .map(CommentDTO::new)
                .collect(Collectors.toList());
        List<CommentDTO> commentsByName = commentRepository.findByAuthorOrderByCreatedAtDesc(user.getName())
                .stream()
                .map(CommentDTO::new)
                .collect(Collectors.toList());
        List<CommentDTO> allComments = new java.util.ArrayList<>();
        allComments.addAll(commentsByEmail);
        allComments.addAll(commentsByName);
        List<CommentDTO> uniqueComments = allComments.stream()
                .collect(java.util.stream.Collectors.toMap(
                        CommentDTO::getId,
                        c -> c,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
                .collect(Collectors.toList());

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("userId", user.getId());
        payload.put("name", user.getName());
        payload.put("email", user.getEmail());
        payload.put("profileImage", user.getProfileImage()); // may be null
        payload.put("bio", user.getUserProfile() != null ? user.getUserProfile().getBio() : null); // UserProfile에서 가져오기
        payload.put("posts", uniquePosts);
        payload.put("comments", uniqueComments);
        return ResponseEntity.ok(payload);
    }

    // ✅ author(이메일 또는 이름)로 사용자 기본 정보 조회
    @GetMapping("/api/users/resolve")
    public ResponseEntity<?> resolveUserByAuthor(@RequestParam("author") String author) {
        // 이메일 우선, 없으면 이름으로 조회
        User user = userRepository.findByEmail(author)
                .or(() -> userRepository.findByName(author))
                .orElse(null);
        if (user == null) {
            return ResponseEntity.ok(Map.of("found", false));
        }
        java.util.Map<String, Object> details = new java.util.HashMap<>();
        details.put("found", true);
        details.put("userId", user.getId());
        details.put("name", user.getName());
        details.put("email", user.getEmail());
        details.put("profileImage", user.getProfileImage()); // may be null
        return ResponseEntity.ok(details);
    }

    // ✅ 여러 작성자(author 문자열 배열)를 한 번에 사용자 정보로 매핑
    @PostMapping("/api/users/resolve-batch")
    public ResponseEntity<?> resolveUsersByAuthors(@RequestBody Map<String, List<String>> body) {
        List<String> authors = body.getOrDefault("authors", List.of());
        Map<String, Map<String, Object>> result = new java.util.HashMap<>();
        for (String author : authors) {
            if (author == null || author.isEmpty()) {
                result.put(author, Map.of("found", false));
                continue;
            }
            User user = userRepository.findByEmail(author)
                    .or(() -> userRepository.findByName(author))
                    .orElse(null);
            if (user == null) {
                result.put(author, Map.of("found", false));
            } else {
                java.util.Map<String, Object> details = new java.util.HashMap<>();
                details.put("found", true);
                details.put("userId", user.getId());
                details.put("name", user.getName());
                details.put("email", user.getEmail());
                details.put("profileImage", user.getProfileImage()); // may be null
                result.put(author, details);
            }
        }
        return ResponseEntity.ok(result);
    }

    // 사용자별 댓글 조회
    @GetMapping("/api/users/me/comments")
    public ResponseEntity<?> getMyComments(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("인증되지 않은 사용자입니다.");
        }
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // 사용자 이름과 이메일 모두로 댓글 검색
        List<CommentDTO> commentsByEmail = commentRepository.findByAuthorOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(CommentDTO::new)
                .collect(Collectors.toList());

        List<CommentDTO> commentsByName = commentRepository.findByAuthorOrderByCreatedAtDesc(user.getName())
                .stream()
                .map(CommentDTO::new)
                .collect(Collectors.toList());

        // 두 결과를 합치고 중복 제거
        List<CommentDTO> allComments = new ArrayList<>();
        allComments.addAll(commentsByEmail);
        allComments.addAll(commentsByName);
        
        // ID 기준으로 중복 제거
        List<CommentDTO> uniqueComments = allComments.stream()
                .collect(Collectors.toMap(
                    CommentDTO::getId,
                    comment -> comment,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .sorted((c1, c2) -> c2.getCreatedAt().compareTo(c1.getCreatedAt()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(uniqueComments);
    }

    // ✅ 사용자 기본 정보 업데이트
    @PostMapping("/api/users/profile/update")
    public ResponseEntity<?> updateUserInfo(@RequestBody UserUpdateRequest request, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User updatedUser = userService.updateUserInfo(userEmail, request);
            return ResponseEntity.ok(MyPageResponse.fromEntity(updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/update-terms")
    public ResponseEntity<?> updateTermsAgreement(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== Update Terms Agreement Debug ===");
            System.out.println("Request: " + request);
            
            Long userId = Long.valueOf(request.get("userId").toString());
            Boolean termsOfServiceAgreed = (Boolean) request.get("termsOfServiceAgreed");
            Boolean privacyPolicyAgreed = (Boolean) request.get("privacyPolicyAgreed");
            Boolean eventPhotoAgreed = (Boolean) request.get("eventPhotoAgreed");
            
            // Validate terms agreement
            if (!termsOfServiceAgreed || !privacyPolicyAgreed || !eventPhotoAgreed) {
                return ResponseEntity.badRequest().body("All terms must be agreed to");
            }
            
            // Find user and update status
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            user.setStatus(UserStatus.ACTIVE_MEMBER); // Change from PRE_MEMBER to ACTIVE_MEMBER
            user = userRepository.save(user);
            
            // Generate JWT token
            String token = jwtTokenProvider.generateToken(user.getEmail());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Terms agreement updated successfully");
            response.put("token", token);
            response.put("email", user.getEmail());
            response.put("name", user.getName());
            response.put("role", user.getRole().name());
            response.put("profileImage", user.getProfileImage());
            
            System.out.println("Terms agreement updated for user: " + user.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Error updating terms agreement: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to update terms agreement: " + e.getMessage());
        }
    }

}