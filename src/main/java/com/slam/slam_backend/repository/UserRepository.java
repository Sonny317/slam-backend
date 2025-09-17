package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByName(String name);
    
    // ✅ 특정 역할을 가진 사용자들 조회
    List<User> findByRole(UserRole role);
    
    // ✅ 멤버십 필드에 특정 지부가 포함된 사용자들 조회
    List<User> findByMembershipContaining(String branchName);
    
    // ✅ 정확한 멤버십 매칭을 위한 쿼리
    @Query("SELECT u FROM User u WHERE u.membership = :branchName")
    List<User> findByExactMembership(@Param("branchName") String branchName);
    
    // ✅ 멤버십이 null이 아닌 사용자들 조회
    @Query("SELECT u FROM User u WHERE u.membership IS NOT NULL AND u.membership != ''")
    List<User> findUsersWithMembership();
    
    // ✅ 특정 지부의 사용자들을 UserProfile과 함께 조회
    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.userProfile " +
           "WHERE u.membership = :branchName")
    List<User> findByExactMembershipWithProfile(@Param("branchName") String branchName);
    
    // ✅ 모든 사용자를 UserProfile과 함께 조회
    @Query("SELECT u FROM User u " +
           "LEFT JOIN FETCH u.userProfile")
    List<User> findAllWithProfile();
}
