package com.slam.slam_backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import com.slam.slam_backend.entity.UserRole;

/**
 * 스태프 임명 요청을 위한 DTO
 */
@Data
public class StaffAssignmentRequest {

    /**
     * 임명할 사용자의 ID
     */
    @NotNull(message = "사용자 ID가 필요합니다.")
    private Long userId;

    /**
     * 임명할 역할
     */
    @NotNull(message = "역할을 선택해주세요.")
    private UserRole targetRole;

    /**
     * 임명 사유 (선택사항)
     */
    private String reason;
}
