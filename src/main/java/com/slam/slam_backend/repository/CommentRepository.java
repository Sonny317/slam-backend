package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // 게시글별 댓글 조회 (최신순)
    List<Comment> findByPostIdOrderByCreatedAtDesc(Long postId);
} 