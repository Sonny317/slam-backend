package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.MembershipApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipApplicationRepository extends JpaRepository<MembershipApplication, Long> {
}