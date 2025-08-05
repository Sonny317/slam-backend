package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    // 카테고리별 게시글 조회
    List<Post> findByCategoryOrderByCreatedAtDesc(String category);
    
    // 고정 게시글 조회
    List<Post> findByIsPinnedTrueOrderByCreatedAtDesc();
    
    // 일반 게시글 조회 (고정 게시글 제외)
    List<Post> findByIsPinnedFalseOrderByCreatedAtDesc();
    
    // 조회수 증가를 위한 네이티브 쿼리
    @Query("UPDATE Post p SET p.views = p.views + 1 WHERE p.id = :postId")
    void incrementViews(@Param("postId") Long postId);
    
    // 좋아요 증가를 위한 네이티브 쿼리
    @Query("UPDATE Post p SET p.likes = p.likes + 1 WHERE p.id = :postId")
    void incrementLikes(@Param("postId") Long postId);
    
    // 작성자별 게시글 조회
    List<Post> findByAuthorOrderByCreatedAtDesc(String author);
} 