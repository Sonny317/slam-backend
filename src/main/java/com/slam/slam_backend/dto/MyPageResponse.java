package com.slam.slam_backend.dto;

import com.slam.slam_backend.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyPageResponse {
    private Long userId;
    private String name;
    private String email;
    private String bio;
    private String profileImage;
    private String membership;
    // 나중에 게시글, 댓글 목록도 여기에 추가할 수 있습니다.

    // User 엔티티를 MyPageResponse DTO로 변환하는 정적 메소드
    public static MyPageResponse fromEntity(User user) {
        return MyPageResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .bio(user.getBio())
                .profileImage(user.getProfileImage())
                .membership(user.getMembership())
                .build();
    }
}
