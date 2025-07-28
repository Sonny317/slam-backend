package com.slam.slam_backend.dto;

import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.UserMembership;
import lombok.Builder;
import lombok.Getter;

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
    private List<String> memberships; // ✅ String에서 List<String>으로 변경
    // 나중에 게시글, 댓글 목록도 여기에 추가할 수 있습니다.

    /// User 엔티티를 MyPageResponse DTO로 변환하는 정적 메소드
    public static MyPageResponse fromEntity(User user) {
        // ✅ 사용자가 가진 모든 멤버십의 지부 이름만 추출하여 리스트로 만듭니다.
        List<String> activeMemberships = user.getMemberships().stream()
                .filter(m -> "ACTIVE".equals(m.getStatus()))
                .map(UserMembership::getBranchName)
                .collect(Collectors.toList());

        return MyPageResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .bio(user.getBio())
                .profileImage(user.getProfileImage())
                .memberships(activeMemberships) // ✅ 추출한 리스트를 담아줍니다.
                .build();
    }
}
