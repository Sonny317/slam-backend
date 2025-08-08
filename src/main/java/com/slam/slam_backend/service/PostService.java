package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.PostDTO;
import com.slam.slam_backend.dto.CommentDTO;
import com.slam.slam_backend.entity.Post;
import com.slam.slam_backend.entity.PostLike;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.Comment;
import com.slam.slam_backend.repository.PostRepository;
import com.slam.slam_backend.repository.CommentRepository;
import com.slam.slam_backend.repository.PostLikeRepository;
import com.slam.slam_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private UserRepository userRepository;

    // 모든 게시글 조회
    public List<PostDTO> getAllPosts() {
        return postRepository.findAll().stream()
                .map(PostDTO::new)
                .collect(Collectors.toList());
    }

    // 카테고리별 게시글 조회
    public List<PostDTO> getPostsByCategory(String category) {
        return postRepository.findByCategoryOrderByCreatedAtDesc(category).stream()
                .map(PostDTO::new)
                .collect(Collectors.toList());
    }

    // 고정 게시글 조회
    public List<PostDTO> getPinnedPosts() {
        return postRepository.findByIsPinnedTrueOrderByCreatedAtDesc().stream()
                .map(PostDTO::new)
                .collect(Collectors.toList());
    }

    // 일반 게시글 조회
    public List<PostDTO> getRegularPosts() {
        return postRepository.findByIsPinnedFalseOrderByCreatedAtDesc().stream()
                .map(PostDTO::new)
                .collect(Collectors.toList());
    }

    // 게시글 상세 조회 (조회수 증가)
    @Transactional
    public PostDTO getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        // 조회수 증가
        post.setViews(post.getViews() + 1);
        postRepository.save(post);
        
        return new PostDTO(post);
    }

    // 게시글 생성
    @Transactional
    public PostDTO createPost(PostDTO postDTO) {
        Post post = new Post();
        post.setTitle(postDTO.getTitle());
        post.setContent(postDTO.getContent());
        post.setSummary(postDTO.getSummary());
        post.setCategory(postDTO.getCategory());
        post.setAuthor(postDTO.getAuthor());
        post.setCreatedAt(java.time.LocalDateTime.now());
        post.setIsPinned(false);
        post.setViews(0);
        post.setLikes(0);
        
        Post savedPost = postRepository.save(post);
        return new PostDTO(savedPost);
    }

    // 좋아요 토글 (한 유저당 한 게시물에 1회, 이미 누르면 취소)
    @Transactional
    public boolean toggleLike(Long postId, String userEmail) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return postLikeRepository.findByUser_IdAndPost_Id(user.getId(), postId)
                .map(existing -> {
                    // 이미 좋아요 → 취소
                    postLikeRepository.delete(existing);
                    long likesCount = postLikeRepository.countByPost_Id(postId);
                    post.setLikes((int) likesCount);
                    postRepository.save(post);
                    return false; // now unliked
                })
                .orElseGet(() -> {
                    // 아직 안눌렀음 → 좋아요 추가
                    postLikeRepository.save(new PostLike(user, post));
                    long likesCount = postLikeRepository.countByPost_Id(postId);
                    post.setLikes((int) likesCount);
                    postRepository.save(post);
                    return true; // now liked
                });
    }

    // 댓글 추가
    @Transactional
    public CommentDTO addComment(Long postId, CommentDTO commentDTO) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        Comment comment = new Comment();
        comment.setText(commentDTO.getText());
        comment.setAuthor(commentDTO.getAuthor());
        comment.setCreatedAt(java.time.LocalDateTime.now());
        comment.setPost(post);
        
        Comment savedComment = commentRepository.save(comment);
        return new CommentDTO(savedComment);
    }

    // 게시글별 댓글 조회
    public List<CommentDTO> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId).stream()
                .map(CommentDTO::new)
                .collect(Collectors.toList());
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        postRepository.delete(post);
    }


} 