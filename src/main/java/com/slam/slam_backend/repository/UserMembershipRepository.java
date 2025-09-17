package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.UserMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserMembershipRepository extends JpaRepository<UserMembership, Long> {
    // ✅ 사용자와 지부 이름으로 멤버십 조회
    List<UserMembership> findByUserAndBranchName(User user, String branchName);
    
    // ✅ 사용자 ID와 지부 이름으로 멤버십 조회 (대안)
    List<UserMembership> findByUserIdAndBranchName(Long userId, String branchName);

    // ✅ 지부별 ACTIVE 멤버십 전체 조회 (대소문자 무시)
    List<UserMembership> findByBranchNameIgnoreCaseAndStatusIgnoreCase(String branchName, String status);
    
    // ✅ 지부별 ACTIVE 멤버십 수 조회
    long countByBranchNameIgnoreCaseAndStatusIgnoreCase(String branchName, String status);
    
    // ✅ 지부별 ACTIVE 멤버십을 User와 UserProfile과 함께 조회
    @Query("SELECT um FROM UserMembership um " +
           "LEFT JOIN FETCH um.user u " +
           "LEFT JOIN FETCH u.userProfile " +
           "WHERE LOWER(um.branchName) = LOWER(:branchName) AND LOWER(um.status) = LOWER(:status)")
    List<UserMembership> findByBranchNameIgnoreCaseAndStatusIgnoreCaseWithProfile(@Param("branchName") String branchName, @Param("status") String status);
}