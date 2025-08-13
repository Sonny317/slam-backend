package com.slam.slam_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "finance_transactions")
public class FinanceTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String branchName; // 지부명

    @Column(nullable = false)
    private String type; // "expense" | "revenue"

    @Column(nullable = false)
    private LocalDate date; // 거래일

    @Column(nullable = false)
    private String item; // 항목명

    @Column(nullable = false)
    private Integer amount; // 금액 (NTD)

    @Column
    private String eventTitle; // 관련 이벤트명 (선택)

    @Column
    private String receiptUrl; // 영수증 URL (선택)

    @Column
    private String submittedBy; // 등록자명 (선택)

    @Column
    private String status; // 지출의 경우: Pending | Reimbursed

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null && "expense".equalsIgnoreCase(type)) {
            status = "Pending";
        }
    }
}


