package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.PostDTO;
import com.slam.slam_backend.dto.CommentDTO;
import com.slam.slam_backend.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = {"http://localhost:3000", "https://slam-taipei.vercel.app"})
public class PostController {

    @Autowired
    private PostService postService;

    // 모든 게시글 조회
    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllPosts() {
        List<PostDTO> posts = postService.getAllPosts();
        return ResponseEntity.ok(posts);
    }

    // 카테고리별 게시글 조회
    @GetMapping("/category/{category}")
    public ResponseEntity<List<PostDTO>> getPostsByCategory(@PathVariable String category) {
        List<PostDTO> posts = postService.getPostsByCategory(category);
        return ResponseEntity.ok(posts);
    }

    // 고정 게시글 조회
    @GetMapping("/pinned")
    public ResponseEntity<List<PostDTO>> getPinnedPosts() {
        List<PostDTO> posts = postService.getPinnedPosts();
        return ResponseEntity.ok(posts);
    }

    // 일반 게시글 조회
    @GetMapping("/regular")
    public ResponseEntity<List<PostDTO>> getRegularPosts() {
        List<PostDTO> posts = postService.getRegularPosts();
        return ResponseEntity.ok(posts);
    }

    // 게시글 상세 조회 (조회수 증가)
    @GetMapping("/{postId}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long postId) {
        PostDTO post = postService.getPostById(postId);
        return ResponseEntity.ok(post);
    }

    // 게시글 생성
    @PostMapping
    public ResponseEntity<PostDTO> createPost(@RequestBody PostDTO postDTO) {
        PostDTO createdPost = postService.createPost(postDTO);
        return ResponseEntity.ok(createdPost);
    }

    // 좋아요 토글 (이미 있으면 취소)
    @PostMapping("/{postId}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long postId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        boolean liked = postService.toggleLike(postId, authentication.getName());
        return ResponseEntity.ok(java.util.Map.of("liked", liked));
    }

    // 댓글 추가
    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentDTO> addComment(@PathVariable Long postId, @RequestBody CommentDTO commentDTO, Authentication authentication) {
        String authorEmail = authentication != null && authentication.isAuthenticated() ? authentication.getName() : commentDTO.getAuthor();
        CommentDTO createdComment = postService.addComment(postId, commentDTO, authorEmail);
        return ResponseEntity.ok(createdComment);
    }

    // 게시글별 댓글 조회
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentDTO>> getCommentsByPostId(@PathVariable Long postId) {
        List<CommentDTO> comments = postService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    // 게시글 삭제 (작성자 또는 관리자만 가능)
    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePost(@PathVariable Long postId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Authentication required");
        }
        
        try {
            boolean deleted = postService.deletePost(postId, authentication.getName());
            if (deleted) {
                return ResponseEntity.ok("Post deleted successfully");
            } else {
                return ResponseEntity.status(403).body("You don't have permission to delete this post");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to delete post: " + e.getMessage());
        }
    }

    // Poll 투표
    @PostMapping("/{postId}/poll/vote")
    public ResponseEntity<?> voteOnPoll(@PathVariable Long postId, @RequestBody Map<String, Integer> request, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Authentication required");
        }
        
        try {
            int optionIndex = request.get("optionIndex");
            Map<String, Object> result = postService.voteOnPoll(postId, authentication.getName(), optionIndex);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to vote: " + e.getMessage());
        }
    }

} 