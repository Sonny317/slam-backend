package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.MembershipRequest;
import com.slam.slam_backend.entity.*;
import com.slam.slam_backend.repository.MembershipApplicationRepository;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.repository.UserMembershipRepository;
import com.slam.slam_backend.repository.EventRepository;
import com.slam.slam_backend.repository.EventRsvpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import com.slam.slam_backend.dto.ApplicationDTO;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final UserMembershipRepository userMembershipRepository;
    private final EventRepository eventRepository;
    private final EventRsvpRepository eventRsvpRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EventService eventService;

    @Transactional
    public MembershipApplication applyForMembership(String userEmail, MembershipRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

        // UserProfile 가져오기 (없으면 새로 생성)
        UserProfile profile = user.getUserProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setUserProfile(profile);
        }

        // --- Single Source of Truth: UserProfile 테이블에 직접 업데이트 ---
        updateProfileFromRequest(profile, request);

        // 신청서는 이제 참조용/히스토리용으로만 저장
        MembershipApplication application = MembershipApplication.builder()
                .user(user)
                .selectedBranch(request.getSelectedBranch())
                .status("payment_pending") // 초기 상태는 '결제 대기'
                .build();

        applicationRepository.save(application);
        userRepository.save(user); // UserProfile 변경사항을 저장하기 위해 User를 저장

        return application;
    }
    
    // 중복 로직을 처리하는 private helper method
    private void updateProfileFromRequest(UserProfile profile, MembershipRequest request) {
        if (request.getUserType() != null && !request.getUserType().trim().isEmpty()) {
            profile.setUserType(request.getUserType());
        }
        if (request.getStudentId() != null && !request.getStudentId().trim().isEmpty()) {
            profile.setStudentId(request.getStudentId());
        }
        if (request.getMajor() != null && !request.getMajor().trim().isEmpty()) {
            profile.setMajor(request.getMajor());
        }
        if (request.getOtherMajor() != null && !request.getOtherMajor().trim().isEmpty()) {
            profile.setOtherMajor(request.getOtherMajor());
        }
        if (request.getProfessionalStatus() != null && !request.getProfessionalStatus().trim().isEmpty()) {
            profile.setProfessionalStatus(request.getProfessionalStatus());
        }
        if (request.getCountry() != null && !request.getCountry().trim().isEmpty()) {
            profile.setCountry(request.getCountry());
            profile.setNationality(request.getCountry()); // nationality도 동기화
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            profile.setPhone(request.getPhone());
        }
        if (request.getFoodAllergies() != null && !request.getFoodAllergies().trim().isEmpty()) {
            profile.setFoodAllergies(request.getFoodAllergies());
        }
        if (request.getPaymentMethod() != null && !request.getPaymentMethod().trim().isEmpty()) {
            profile.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getBankLast5() != null && !request.getBankLast5().trim().isEmpty()) {
            profile.setBankLast5(request.getBankLast5());
        }
    }


    @Transactional
    public MembershipApplication applyForMembershipWithEvent(String userEmail, MembershipRequest request, Long eventId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));
        
        // UserProfile 업데이트
        UserProfile profile = user.getUserProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setUserProfile(profile);
        }
        updateProfileFromRequest(profile, request);

        MembershipApplication application = MembershipApplication.builder()
                .user(user)
                .selectedBranch(request.getSelectedBranch())
                .eventId(eventId)
                .status("payment_pending")
                .build();

        applicationRepository.save(application);
        userRepository.save(user); // UserProfile 변경사항 저장

        return application;
    }

    @Transactional(readOnly = true)
    public MembershipApplication findMyLatestApplication(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));
        return applicationRepository.findTopByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void updateProfileFromMembership(String userEmail, MembershipRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));
        
        UserProfile profile = user.getUserProfile();
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
            user.setUserProfile(profile);
        }

        updateProfileFromRequest(profile, request);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDTO> findAllApplications() {
        return applicationRepository.findAll().stream()
                .map(ApplicationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveApplication(Long applicationId) {
        MembershipApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신청서를 찾을 수 없습니다: " + applicationId));

        application.setStatus("APPROVED");
        applicationRepository.save(application);

        // 3. ✅ 새로운 UserMembership 객체를 생성하여 DB에 저장합니다.
        User user = application.getUser();
        UserMembership newMembership = UserMembership.builder()
                .user(user)
                .branchName(application.getSelectedBranch())
                .status("ACTIVE")
                .build();

        userMembershipRepository.save(newMembership);

        // 4. ✅ 사용자 엔티티의 membership 필드도 동기화 (단일 표기용)
        user.setMembership(application.getSelectedBranch());
        
        // 5. ✅ 사용자 상태를 ACTIVE_MEMBER로 변경하고 membershipType 설정
        user.setStatus(UserStatus.ACTIVE_MEMBER);
        
        // ✅ 멤버십 타입 설정 (Regular Meet 참석 가능하도록)
        user.setMembershipType(MembershipType.FULL_SEMESTER); 
        
        userRepository.save(user);
        
        // 6. ✅ 티켓 구매 신청인 경우 RSVP 생성
        // UserProfile에서 paymentMethod 정보를 가져와서 확인
        String paymentMethod = null;
        if (user.getUserProfile() != null) {
            paymentMethod = user.getUserProfile().getPaymentMethod();
        }
        
        if ("Ticket Purchase".equals(paymentMethod) && application.getEventId() != null) {
            try {
                // ✅ 신청서에 저장된 이벤트 ID로 RSVP 생성
                Event event = eventRepository.findById(application.getEventId())
                    .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다: " + application.getEventId()));
                
                EventRsvp rsvp = new EventRsvp();
                rsvp.setUser(user);
                rsvp.setEvent(event);
                rsvp.setAttending(true);
                rsvp.setAfterParty(false);
                rsvp.setAttended(false);
                
                eventRsvpRepository.save(rsvp);
                System.out.println("티켓 구매 승인: RSVP 생성 완료 - Event ID: " + application.getEventId());
            } catch (Exception e) {
                System.err.println("티켓 구매 승인 중 RSVP 생성 실패: " + e.getMessage());
            }
        }
        
        notificationService.createMembershipNotification(user.getEmail(), application.getSelectedBranch(), true);
    }

    @Transactional
    public void rejectApplication(Long applicationId) {
        MembershipApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신청서를 찾을 수 없습니다: " + applicationId));

        application.setStatus("REJECTED");
        applicationRepository.save(application);
        
        User user = application.getUser();
        notificationService.createMembershipNotification(user.getEmail(), application.getSelectedBranch(), false);
    }
    
    // (이하 getMembershipPricing, hasPendingTicketApplication 등 나머지 메소드는 변경 필요 없음)
    // ... 기존 코드 유지 ...
    @Transactional(readOnly = true)
    public Map<String, Object> getMembershipPricing(String branch) {
        Map<String, Object> pricing = new HashMap<>();
        
        long currentMembers = userMembershipRepository.countByBranchNameIgnoreCaseAndStatusIgnoreCase(branch, "ACTIVE");
        
        Event latestEvent = eventRepository.findTopByBranchOrderByEventDateTimeDesc(branch)
            .orElse(null);
        
        int earlyBirdCap = 20;
        int earlyBirdPrice = 800;
        int regularPrice = 900;
        int totalCapacity = 80;
        String earlyBirdDeadline = "2025-03-15T23:59:59";
        String regularDeadline = "2025-09-12T23:59:59";
        
        if (latestEvent != null) {
            if (latestEvent.getEarlyBirdPrice() != null) {
                earlyBirdPrice = latestEvent.getEarlyBirdPrice();
            }
            regularPrice = latestEvent.getPrice();
            if (latestEvent.getEarlyBirdCapacity() != null) {
                earlyBirdCap = latestEvent.getEarlyBirdCapacity();
            }
            totalCapacity = latestEvent.getCapacity();
            
            if (latestEvent.getRegistrationDeadline() != null) {
                earlyBirdDeadline = latestEvent.getRegistrationDeadline().toString();
            }
            
            if (latestEvent.getEventDateTime() != null) {
                regularDeadline = latestEvent.getEventDateTime().toString();
            }
        }
        
        boolean isEarlyBirdActive = latestEvent != null && 
                                   latestEvent.getEarlyBirdPrice() != null && 
                                   currentMembers < earlyBirdCap;
        
        int currentPrice = isEarlyBirdActive ? earlyBirdPrice : regularPrice;
        
        pricing.put("currentMembers", currentMembers);
        pricing.put("earlyBirdCap", earlyBirdCap);
        pricing.put("earlyBirdPrice", earlyBirdPrice);
        pricing.put("regularPrice", regularPrice);
        pricing.put("currentPrice", currentPrice);
        pricing.put("isEarlyBirdActive", isEarlyBirdActive);
        pricing.put("totalCapacity", totalCapacity);
        pricing.put("earlyBirdDeadline", earlyBirdDeadline);
        pricing.put("regularDeadline", regularDeadline);
        
        int totalValue = regularPrice + 600;
        pricing.put("totalValue", totalValue);
        
        if (latestEvent != null && latestEvent.getBankAccount() != null) {
            String bankName = latestEvent.getBankName() != null && !latestEvent.getBankName().trim().isEmpty() 
                ? latestEvent.getBankName() 
                : getDefaultBankName(branch);
            String accountName = latestEvent.getAccountName() != null && !latestEvent.getAccountName().trim().isEmpty() 
                ? latestEvent.getAccountName() 
                : getDefaultAccountName(branch);
            
            String combinedBankInfo = bankName + " - " + latestEvent.getBankAccount() + " - " + accountName;
            pricing.put("bankAccount", combinedBankInfo);
        } else {
            String defaultBankAccount = getDefaultBankAccount(branch);
            pricing.put("bankAccount", defaultBankAccount);
        }
        
        return pricing;
    }

    @Transactional(readOnly = true)
    public boolean hasPendingTicketApplication(String userEmail, Long eventId) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) return false;
        
        boolean hasPending = applicationRepository.existsByUserAndPaymentMethodAndStatusAndEventId(
            user, "Ticket Purchase", "payment_pending", eventId);
        
        return hasPending;
    }
    
    private String getDefaultBankAccount(String branch) {
        switch (branch.toUpperCase()) {
            case "NCCU":
                return "(822) Cathay United Bank - 123-456-7890 - SLAM NCCU";
            case "NTU":
                return "(700) China Post - 098-765-4321 - SLAM NTU";
            case "TAIPEI":
                return "(812) Taiwan Cooperative Bank - 555-123-9876 - SLAM TAIPEI";
            default:
                return "(822) Cathay United Bank - 123-456-7890 - SLAM NCCU";
        }
    }
    
    private String getDefaultBankName(String branch) {
        switch (branch.toUpperCase()) {
            case "NCCU":
                return "(822) Cathay United Bank";
            case "NTU":
                return "(700) China Post";
            case "TAIPEI":
                return "(812) Taiwan Cooperative Bank";
            default:
                return "(822) Cathay United Bank";
        }
    }
    
    private String getDefaultAccountName(String branch) {
        switch (branch.toUpperCase()) {
            case "NCCU":
                return "SLAM NCCU";
            case "NTU":
                return "SLAM NTU";
            case "TAIPEI":
                return "SLAM TAIPEI";
            default:
                return "SLAM NCCU";
        }
    }
}