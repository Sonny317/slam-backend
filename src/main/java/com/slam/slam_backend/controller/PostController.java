package com.slam.slam_backend.controller;

import com.slam.slam_backend.dto.PostDTO;
import com.slam.slam_backend.dto.CommentDTO;
import com.slam.slam_backend.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // 좋아요 증가
    @PostMapping("/{postId}/like")
    public ResponseEntity<String> incrementLikes(@PathVariable Long postId) {
        postService.incrementLikes(postId);
        return ResponseEntity.ok("Likes incremented successfully");
    }

    // 댓글 추가
    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentDTO> addComment(@PathVariable Long postId, @RequestBody CommentDTO commentDTO) {
        CommentDTO createdComment = postService.addComment(postId, commentDTO);
        return ResponseEntity.ok(createdComment);
    }

    // 게시글별 댓글 조회
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentDTO>> getCommentsByPostId(@PathVariable Long postId) {
        List<CommentDTO> comments = postService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    // 게시글 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.ok("Post deleted successfully");
    }


} 