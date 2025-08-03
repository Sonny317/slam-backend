package com.slam.slam_backend.dto;

import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.UserMembership;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class MyPageResponse {
    private Long userId;
    private String name;
    private String email;
    private String bio;
    private String profileImage;
    private String role; // ✅ role 필드 추가
    private List<String> memberships; // ✅ String에서 List<String>으로 변경
    // 나중에 게시글, 댓글 목록도 여기에 추가할 수 있습니다.

    /// User 엔티티를 MyPageResponse DTO로 변환하는 정적 메소드
    public static MyPageResponse fromEntity(User user) {
        // ✅ 단일 멤버십 문자열을 리스트로 변환합니다.
        List<String> memberships = new ArrayList<>();
        if (user.getMembership() != null && !user.getMembership().isEmpty()) {
            // "ACTIVE_NCCU" 형태에서 지부 이름만 추출
            String membership = user.getMembership();
            if (membership.contains("_")) {
                String[] parts = membership.split("_");
                if (parts.length >= 2) {
                    String status = parts[0];
                    String branchName = parts[1];
                    if ("ACTIVE".equals(status)) {
                        memberships.add(branchName);
                    }
                }
            } else {
                // 단순한 지부 이름인 경우
                memberships.add(membership);
            }
        }
        
        // 기존 멤버십 컬렉션에서도 ACTIVE 상태인 것들을 추가
        List<String> activeMemberships = user.getMemberships().stream()
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .map(UserMembership::getBranchName)
                .collect(Collectors.toList());
        
        memberships.addAll(activeMemberships);

        return MyPageResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .bio(user.getBio())
                .profileImage(user.getProfileImage())
                .role(user.getRole()) // ✅ 역할 정보 추가
                .memberships(memberships) // ✅ 최종 멤버십 리스트를 담아줍니다.
                .build();
    }
}
