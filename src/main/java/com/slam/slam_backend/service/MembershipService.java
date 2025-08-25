package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.MembershipRequest;
import com.slam.slam_backend.entity.MembershipApplication;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.UserStatus;
import com.slam.slam_backend.entity.MembershipType; // ✅ MembershipType enum import 추가
import com.slam.slam_backend.repository.MembershipApplicationRepository;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.entity.UserMembership; // ✅ 임포트 추가
import com.slam.slam_backend.repository.UserMembershipRepository; // ✅ 임포트 추가
import com.slam.slam_backend.entity.Event; // ✅ Event 엔티티 임포트
import com.slam.slam_backend.entity.EventRsvp; // ✅ EventRsvp 엔티티 임포트
import com.slam.slam_backend.repository.EventRepository; // ✅ EventRepository 임포트
import com.slam.slam_backend.repository.EventRsvpRepository; // ✅ EventRsvpRepository 임포트
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import com.slam.slam_backend.dto.ApplicationDTO; // ✅ DTO 임포트
import lombok.Setter;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors; // ✅ Collectors 임포트

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

        // ✅ Single Source of Truth: User 테이블을 직접 업데이트
        if (request.getUserType() != null && !request.getUserType().trim().isEmpty()) {
            user.setUserType(request.getUserType());
        }
        if (request.getStudentId() != null && !request.getStudentId().trim().isEmpty()) {
            user.setStudentId(request.getStudentId());
        }
        if (request.getMajor() != null && !request.getMajor().trim().isEmpty()) {
            user.setMajor(request.getMajor());
        }
        if (request.getOtherMajor() != null && !request.getOtherMajor().trim().isEmpty()) {
            user.setOtherMajor(request.getOtherMajor());
        }
        if (request.getProfessionalStatus() != null && !request.getProfessionalStatus().trim().isEmpty()) {
            user.setProfessionalStatus(request.getProfessionalStatus());
        }
        if (request.getCountry() != null && !request.getCountry().trim().isEmpty()) {
            user.setCountry(request.getCountry());
            user.setNationality(request.getCountry()); // nationality도 동기화
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone());
        }
        if (request.getFoodAllergies() != null && !request.getFoodAllergies().trim().isEmpty()) {
            user.setFoodAllergies(request.getFoodAllergies());
        }
        if (request.getPaymentMethod() != null && !request.getPaymentMethod().trim().isEmpty()) {
            user.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getBankLast5() != null && !request.getBankLast5().trim().isEmpty()) {
            user.setBankLast5(request.getBankLast5());
        }

        // User 테이블 업데이트
        userRepository.save(user);

        // ✅ 신청서는 이제 참조용/히스토리용으로만 저장
        MembershipApplication application = MembershipApplication.builder()
                .user(user)
                .selectedBranch(request.getSelectedBranch())
                .userType(request.getUserType())
                .studentId(request.getStudentId())
                .major(request.getMajor())
                .otherMajor(request.getOtherMajor())
                .professionalStatus(request.getProfessionalStatus())
                .country(request.getCountry())
                .phone(request.getPhone())
                .foodAllergies(request.getFoodAllergies())
                .paymentMethod(request.getPaymentMethod())
                .bankLast5(request.getBankLast5())
                .status("payment_pending") // 초기 상태는 '결제 대기'
                .build();

        return applicationRepository.save(application);
    }

    // ✅ 이벤트 ID를 포함한 멤버십 신청 처리 (티켓 구매용)
    @Transactional
    public MembershipApplication applyForMembershipWithEvent(String userEmail, MembershipRequest request, Long eventId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

        // ✅ 신청서 생성 (이벤트 ID 포함)
        MembershipApplication application = MembershipApplication.builder()
                .user(user)
                .selectedBranch(request.getSelectedBranch())
                .userType(request.getUserType())
                .studentId(request.getStudentId())
                .major(request.getMajor())
                .otherMajor(request.getOtherMajor())
                .professionalStatus(request.getProfessionalStatus())
                .country(request.getCountry())
                .phone(request.getPhone())
                .foodAllergies(request.getFoodAllergies())
                .paymentMethod(request.getPaymentMethod())
                .bankLast5(request.getBankLast5())
                .eventId(eventId) // ✅ 이벤트 ID 저장
                .status("payment_pending")
                .build();

        return applicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public MembershipApplication findMyLatestApplication(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));
        return applicationRepository.findTopByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void updateProfileFromMembership(String userEmail, com.slam.slam_backend.dto.MembershipRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

        // ✅ 모든 멤버십 관련 필드를 User 테이블에 업데이트
        if (request.getUserType() != null && !request.getUserType().trim().isEmpty()) {
            user.setUserType(request.getUserType());
        }
        if (request.getStudentId() != null && !request.getStudentId().trim().isEmpty()) {
            user.setStudentId(request.getStudentId());
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone());
        }
        if (request.getMajor() != null && !request.getMajor().trim().isEmpty()) {
            user.setMajor(request.getMajor());
        }
        if (request.getOtherMajor() != null && !request.getOtherMajor().trim().isEmpty()) {
            user.setOtherMajor(request.getOtherMajor());
        }
        if (request.getProfessionalStatus() != null && !request.getProfessionalStatus().trim().isEmpty()) {
            user.setProfessionalStatus(request.getProfessionalStatus());
        }
        if (request.getCountry() != null && !request.getCountry().trim().isEmpty()) {
            user.setCountry(request.getCountry());
            user.setNationality(request.getCountry());
        }
        if (request.getFoodAllergies() != null && !request.getFoodAllergies().trim().isEmpty()) {
            user.setFoodAllergies(request.getFoodAllergies());
        }
        if (request.getPaymentMethod() != null && !request.getPaymentMethod().trim().isEmpty()) {
            user.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getBankLast5() != null && !request.getBankLast5().trim().isEmpty()) {
            user.setBankLast5(request.getBankLast5());
        }

        userRepository.save(user);
    }

    // ✅ 추가: 모든 멤버십 신청 목록을 조회하는 메소드
    // ✅ 반환 타입을 List<ApplicationDTO>로 수정
    @Transactional(readOnly = true)
    public List<ApplicationDTO> findAllApplications() {
        return applicationRepository.findAll().stream()
                .map(ApplicationDTO::fromEntity) // ✅ Entity를 DTO로 변환
                .collect(Collectors.toList());
    }

    // ✅ 추가: 멤버십 신청을 승인하는 메소드
    @Transactional
    public void approveApplication(Long applicationId) {
        MembershipApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신청서를 찾을 수 없습니다: " + applicationId));

        // 1. 신청서의 상태를 'APPROVED'로 변경
        application.setStatus("APPROVED");
        applicationRepository.save(application);

        // 2. ✅ 새로운 UserMembership 객체를 생성하여 DB에 저장합니다.
        User user = application.getUser();
        UserMembership newMembership = UserMembership.builder()
                .user(user)
                .branchName(application.getSelectedBranch())
                .status("ACTIVE")
                .build();

        userMembershipRepository.save(newMembership);

        // 3. ✅ 사용자 엔티티의 membership 필드도 동기화 (단일 표기용)
        user.setMembership(application.getSelectedBranch());
        
        // 4. ✅ 신청서의 상세 정보를 User 테이블에도 저장
        if (application.getStudentId() != null && !application.getStudentId().trim().isEmpty()) {
            user.setStudentId(application.getStudentId());
        }
        if (application.getPhone() != null && !application.getPhone().trim().isEmpty()) {
            user.setPhone(application.getPhone());
        }
        if (application.getMajor() != null && !application.getMajor().trim().isEmpty()) {
            user.setMajor(application.getMajor());
        }
        
        // 5. ✅ 사용자 상태를 ACTIVE_MEMBER로 변경하고 membershipType 설정
        user.setStatus(UserStatus.ACTIVE_MEMBER);
        
        // ✅ 멤버십 타입 설정 (Regular Meet 참석 가능하도록)
        user.setMembershipType(MembershipType.FULL_SEMESTER); 
        
        userRepository.save(user);
        
        // 6. ✅ 티켓 구매 신청인 경우 RSVP 생성
        if ("Ticket Purchase".equals(application.getPaymentMethod()) && application.getEventId() != null) {
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
        
        // 7. 승인 알림 생성
        notificationService.createMembershipNotification(user.getEmail(), application.getSelectedBranch(), true);
    }

    // ✅ 추가: 멤버십 신청을 거부하는 메소드
    @Transactional
    public void rejectApplication(Long applicationId) {
        MembershipApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 신청서를 찾을 수 없습니다: " + applicationId));

        // 신청서의 상태를 'REJECTED'로 변경
        application.setStatus("REJECTED");
        applicationRepository.save(application);
        
        // 거절 알림 생성
        User user = application.getUser();
        notificationService.createMembershipNotification(user.getEmail(), application.getSelectedBranch(), false);
    }

    // ✅ 추가: 멤버십 가격 정보를 제공하는 메소드
    @Transactional(readOnly = true)
    public Map<String, Object> getMembershipPricing(String branch) {
        Map<String, Object> pricing = new HashMap<>();
        
        // 현재 멤버 수 조회
        long currentMembers = userMembershipRepository.countByBranchNameIgnoreCaseAndStatusIgnoreCase(branch, "ACTIVE");
        
        // ✅ 해당 지부의 최신 이벤트에서 가격 정보 가져오기
        Event latestEvent = eventRepository.findTopByBranchOrderByEventDateTimeDesc(branch)
            .orElse(null);
        
        int earlyBirdCap = 20; // 기본값
        int earlyBirdPrice = 800; // 기본값
        int regularPrice = 900; // 기본값
        int totalCapacity = 80; // 기본값
        String earlyBirdDeadline = "2025-03-15T23:59:59"; // 기본값
        String regularDeadline = "2025-09-12T23:59:59"; // 기본값
        
        if (latestEvent != null) {
            // 이벤트에서 설정된 가격 정보 사용 (null 체크)
            if (latestEvent.getEarlyBirdPrice() != null) {
                earlyBirdPrice = latestEvent.getEarlyBirdPrice();
            }
            regularPrice = latestEvent.getPrice(); // int 타입이므로 null 체크 불필요
            if (latestEvent.getEarlyBirdCapacity() != null) {
                earlyBirdCap = latestEvent.getEarlyBirdCapacity();
            }
            totalCapacity = latestEvent.getCapacity(); // int 타입이므로 null 체크 불필요
            
            // 이벤트의 등록 마감일을 Early Bird deadline으로 사용
            if (latestEvent.getRegistrationDeadline() != null) {
                earlyBirdDeadline = latestEvent.getRegistrationDeadline().toString();
            }
            
            // 이벤트 시작일을 Regular deadline으로 사용
            if (latestEvent.getEventDateTime() != null) {
                regularDeadline = latestEvent.getEventDateTime().toString();
            }
        }
        
        // ✅ 얼리버드 활성화 여부 결정: 얼리버드 가격이 설정되어 있고, 현재 멤버 수가 얼리버드 제한보다 적을 때만 활성화
        boolean isEarlyBirdActive = latestEvent != null && 
                                   latestEvent.getEarlyBirdPrice() != null && 
                                   currentMembers < earlyBirdCap;
        
        // 현재 가격 결정
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
        
        // ✅ Total Value 계산 (Regular Price + 추가 혜택 가치)
        int totalValue = regularPrice + 600; // Regular Price + 600 NTD (추가 혜택 가치)
        pricing.put("totalValue", totalValue);
        
        // ✅ 이벤트의 은행 정보 추가
        System.out.println("🔍 Bank Account Debug - Latest Event: " + (latestEvent != null ? latestEvent.getId() : "null"));
        if (latestEvent != null) {
            System.out.println("🔍 Bank Account Debug - Event Bank Account: " + latestEvent.getBankAccount());
            System.out.println("🔍 Bank Account Debug - Event Bank Name: '" + latestEvent.getBankName() + "' (length: " + (latestEvent.getBankName() != null ? latestEvent.getBankName().length() : "null") + ")");
            System.out.println("🔍 Bank Account Debug - Event Account Name: '" + latestEvent.getAccountName() + "' (length: " + (latestEvent.getAccountName() != null ? latestEvent.getAccountName().length() : "null") + ")");
            System.out.println("🔍 Bank Account Debug - Event Bank Name is null: " + (latestEvent.getBankName() == null));
            System.out.println("🔍 Bank Account Debug - Event Bank Name is empty: " + (latestEvent.getBankName() != null && latestEvent.getBankName().trim().isEmpty()));
        }
        
        if (latestEvent != null && latestEvent.getBankAccount() != null) {
            // 이벤트에서 은행명과 계좌명이 설정되어 있으면 사용, 없으면 기본값 사용
            String bankName = latestEvent.getBankName() != null && !latestEvent.getBankName().trim().isEmpty() 
                ? latestEvent.getBankName() 
                : getDefaultBankName(branch);
            String accountName = latestEvent.getAccountName() != null && !latestEvent.getAccountName().trim().isEmpty() 
                ? latestEvent.getAccountName() 
                : getDefaultAccountName(branch);
            
            String combinedBankInfo = bankName + " - " + latestEvent.getBankAccount() + " - " + accountName;
            System.out.println("🔍 Bank Account Debug - Combined bank info: " + combinedBankInfo);
            pricing.put("bankAccount", combinedBankInfo);
        } else {
            // 기본 은행 정보 (지부별)
            String defaultBankAccount = getDefaultBankAccount(branch);
            System.out.println("🔍 Bank Account Debug - Using default bank account: " + defaultBankAccount);
            pricing.put("bankAccount", defaultBankAccount);
        }
        
        return pricing;
    }

    // ✅ 사용자의 승인 대기 중인 티켓 구매 신청 확인
    @Transactional(readOnly = true)
    public boolean hasPendingTicketApplication(String userEmail, Long eventId) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) return false;
        
        // ✅ 특정 이벤트에 대한 승인 대기 중인 티켓 구매 신청 확인
        boolean hasPending = applicationRepository.existsByUserAndPaymentMethodAndStatusAndEventId(
            user, "Ticket Purchase", "payment_pending", eventId);
        
        // ✅ 디버깅 로그 추가
        System.out.println("🔍 Pending Ticket Check - User: " + userEmail + ", Event ID: " + eventId);
        System.out.println("   - User ID: " + user.getId());
        System.out.println("   - Has Pending Ticket: " + hasPending);
        
        return hasPending;
    }
    
    // ✅ 지부별 기본 은행 정보 반환
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
    
    // ✅ 지부별 기본 은행명 반환
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
    
    // ✅ 지부별 기본 계좌명 반환
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