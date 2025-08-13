package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.FinanceTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, Long> {
    List<FinanceTransaction> findByBranchNameIgnoreCaseOrderByDateDescIdDesc(String branchName);
}


