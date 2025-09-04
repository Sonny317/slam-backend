package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.RegisterRequest;
import com.slam.slam_backend.dto.UserUpdateRequest;
import com.slam.slam_backend.entity.PasswordResetToken;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.UserProfile;
import com.slam.slam_backend.entity.UserRole;
import com.slam.slam_backend.entity.UserStatus;
import com.slam.slam_backend.entity.VerificationCode;
import com.slam.slam_backend.repository.PasswordResetTokenRepository;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.regex.Pattern;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Transactional
    public void sendVerificationCode(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        String code = generateRandomCode();
        verificationCodeRepository.findByEmail(email)
                .ifPresentOrElse(existing -> {
                    existing.setCode(code);
                    existing.setExpiryDate(LocalDateTime.now().plusMinutes(10));
                    verificationCodeRepository.save(existing);
                }, () -> {
                    verificationCodeRepository.save(new VerificationCode(email, code));
                });
        String subject = "[SLAM] Your verification code";
        String text = "To complete your registration, please enter the verification code below:\n\n" + "Verification code: " + code;
        emailService.sendEmail(email, subject, text);
    }

    @Transactional
public User registerUser(RegisterRequest request) {
    System.out.println("=== Register User Debug ===");
    System.out.println("Request email: " + request.getEmail());
    System.out.println("Request isGoogleUser: " + request.isGoogleUser());
    System.out.println("Request googleId: " + request.getGoogleId());
    System.out.println("Request name: " + request.getName());
    System.out.println("Request password: " + (request.getPassword() != null ? "NOT_NULL" : "NULL"));
    System.out.println("Request code: " + (request.getCode() != null ? "NOT_NULL" : "NULL"));
    
    // Google OAuth 사용자인 경우 별도 처리 (인증코드 검증 이전에 체크)
    if (request.isGoogleUser() == true) {
        System.out.println("Processing as Google OAuth user - skipping verification code check");
        return registerGoogleUser(request);
    }
    
    // 추가 체크: googleId가 있거나 password가 비어있으면 Google OAuth 사용자로 처리
    if (request.getGoogleId() != null && !request.getGoogleId().isEmpty()) {
        System.out.println("Google ID detected - processing as Google OAuth user");
        return registerGoogleUser(request);
    }
    
    if (request.getPassword() == null || request.getPassword().isEmpty()) {
        System.out.println("No password provided - processing as Google OAuth user");
        return registerGoogleUser(request);
    }
    
    System.out.println("Processing as regular user - checking verification code");
    
    // 1. 인증코드 검증 및 비밀번호 유효성 검사 (일반 사용자만)
    if (request.getCode() == null || request.getCode().isEmpty()) {
        throw new IllegalArgumentException("Verification code is required for regular users");
    }
    
    VerificationCode storedCode = verificationCodeRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Verification code not issued or expired"));

    if (storedCode.getExpiryDate().isBefore(LocalDateTime.now())) {
        verificationCodeRepository.delete(storedCode);
        throw new IllegalArgumentException("Verification code expired");
    }

    if (!storedCode.getCode().equals(request.getCode())) {
        throw new IllegalArgumentException("Invalid verification code");
    }

    // Google OAuth 사용자가 아닌 경우에만 비밀번호 검증
    if (request.getPassword() != null && !request.getPassword().isEmpty()) {
        validatePassword(request.getPassword());
    }

    // 2. User 객체 생성 (핵심 정보만)
    User user = User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(UserRole.MEMBER)
            .status(UserStatus.PRE_MEMBER)
            .build();

    // 3. UserProfile 객체 생성 및 관계 설정
    UserProfile userProfile = UserProfile.builder()
            .user(user)
            .build();
    
    // 4. User와 UserProfile의 양방향 관계 설정
    user.setUserProfile(userProfile);

    verificationCodeRepository.delete(storedCode);
    
    // 5. User를 저장합니다. Cascade 설정에 의해 UserProfile도 함께 저장됩니다.
    return userRepository.save(user);
}

@Transactional
public User registerGoogleUser(RegisterRequest request) {
    System.out.println("=== Register Google User Debug ===");
    System.out.println("Creating Google user: " + request.getEmail());
    
    // Check if user already exists
    Optional<User> existingUser = userRepository.findByEmail(request.getEmail());
    if (existingUser.isPresent()) {
        User user = existingUser.get();
        // If user exists but is not a Google OAuth user, update their provider info
        if (user.getProvider() == null || !"google".equals(user.getProvider())) {
            System.out.println("Updating existing user with Google OAuth info: " + request.getEmail());
            user.setProvider("google");
            user.setProviderId(request.getGoogleId());
            user.setOauthId(request.getGoogleId());
            return userRepository.save(user);
        } else {
            // User already exists as Google OAuth user
            System.out.println("User already exists as Google OAuth user: " + request.getEmail());
            return user;
        }
    }
    
    // Google OAuth 사용자는 인증코드 검증 없이 회원가입
    User user = User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .password(null) // Google OAuth 사용자는 비밀번호 없음
            .role(UserRole.MEMBER)
            .status(UserStatus.PRE_MEMBER)
            .provider("google")
            .providerId(request.getGoogleId())
            .oauthId(request.getGoogleId())
            .createdAt(LocalDateTime.now()) // 생성 시간 명시적 설정
            .build();

    // UserProfile 객체 생성 및 관계 설정
    UserProfile userProfile = UserProfile.builder()
            .user(user)
            .build();
    
    user.setUserProfile(userProfile);
    
    System.out.println("Creating new Google OAuth user: " + user.getEmail());
    return userRepository.save(user);
}
    
    // (이하 나머지 코드는 이전과 동일합니다)

    private void validatePassword(String password) {
        String passwordRegex = "^(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{6,}$";
        if (!Pattern.matches(passwordRegex, password)) {
            throw new IllegalArgumentException("Password must be at least 6 characters and include special characters.");
        }
    }

    public User login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email does not exist."));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Password does not match.");
        }
        return user;
    }

    private String generateRandomCode() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));
    }

    @Transactional
    public User updateBio(String email, String newBio) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        
        UserProfile userProfile = user.getUserProfile();
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setUser(user);
            user.setUserProfile(userProfile);
        }
        userProfile.setBio(newBio);
        
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfileImage(String email, MultipartFile newProfileImageFile) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (newProfileImageFile == null || newProfileImageFile.isEmpty()) {
            throw new IllegalArgumentException("No image file provided for update.");
        }

        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            Path oldImagePath = Paths.get(uploadDir, user.getProfileImage().replace("/images/", ""));
            Files.deleteIfExists(oldImagePath);
        }

        String filename = UUID.randomUUID() + "_" + newProfileImageFile.getOriginalFilename();
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File destination = new File(uploadDir + File.separator + filename);
        newProfileImageFile.transferTo(destination);

        user.setProfileImage("/images/" + filename);
        return userRepository.save(user);
    }

    @Transactional
    public void createPasswordResetTokenForUser(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }
        String token = UUID.randomUUID().toString();
        PasswordResetToken myToken = new PasswordResetToken(user, token);
        passwordResetTokenRepository.save(myToken);

        String resetUrl = frontendBaseUrl + "/reset-password?token=" + token;
        String subject = "[SLAM] Password reset request";
        String text = "Click the link below to reset your password:\n\n" + resetUrl;

        emailService.sendEmail(user.getEmail(), subject, text);
    }

    @Transactional
    public void changePassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Token"));

        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("Token has expired.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }

    @Transactional
    public void changePasswordForAuthenticatedUser(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public boolean verifyVerificationCode(String email, String code) {
        return verificationCodeRepository.findByEmail(email)
                .map(stored -> {
                    if (stored.getExpiryDate().isBefore(LocalDateTime.now())) {
                        verificationCodeRepository.delete(stored);
                        return false;
                    }
                    return stored.getCode().equals(code);
                })
                .orElse(false);
    }

    @Transactional
    public User updateUserInfo(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        user.setName(request.getName());

        UserProfile profile = user.getUserProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setUserProfile(profile);
        }

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            profile.setPhone(request.getPhone());
        }
        if (request.getMajor() != null && !request.getMajor().trim().isEmpty()) {
            profile.setMajor(request.getMajor());
        }
        if (request.getStudentId() != null && !request.getStudentId().trim().isEmpty()) {
            profile.setStudentId(request.getStudentId());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getInterests() != null) {
            profile.setInterests(request.getInterests());
        }
        if (request.getSpokenLanguages() != null) {
            profile.setSpokenLanguages(request.getSpokenLanguages());
        }
        if (request.getDesiredLanguages() != null) {
            profile.setDesiredLanguages(request.getDesiredLanguages());
        }

        return userRepository.save(user);
    }
}