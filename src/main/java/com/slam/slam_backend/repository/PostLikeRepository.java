package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByUser_IdAndPost_Id(Long userId, Long postId);
    long countByPost_Id(Long postId);
}


