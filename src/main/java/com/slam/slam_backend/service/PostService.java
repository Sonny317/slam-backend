package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.PostDTO;
import com.slam.slam_backend.dto.CommentDTO;
import com.slam.slam_backend.entity.Post;
import com.slam.slam_backend.entity.Poll;
import com.slam.slam_backend.entity.PollOption;
import com.slam.slam_backend.entity.PostLike;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.Comment;
import com.slam.slam_backend.repository.PostRepository;
import com.slam.slam_backend.repository.PollRepository;
import com.slam.slam_backend.repository.PollOptionRepository;
import com.slam.slam_backend.repository.PollVoteRepository;
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

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private PollOptionRepository pollOptionRepository;

    @Autowired
    private PollVoteRepository pollVoteRepository;

    // 모든 게시글 조회
    public List<PostDTO> getAllPosts() {
        return postRepository.findAll().stream()
                .map(p -> enrichAuthorDisplay(new PostDTO(p)))
                .collect(Collectors.toList());
    }

    // 카테고리별 게시글 조회
    public List<PostDTO> getPostsByCategory(String category) {
        return postRepository.findByCategoryOrderByCreatedAtDesc(category).stream()
                .map(p -> enrichAuthorDisplay(new PostDTO(p)))
                .collect(Collectors.toList());
    }

    // 고정 게시글 조회
    public List<PostDTO> getPinnedPosts() {
        return postRepository.findByIsPinnedTrueOrderByCreatedAtDesc().stream()
                .map(p -> enrichAuthorDisplay(new PostDTO(p)))
                .collect(Collectors.toList());
    }

    // 일반 게시글 조회
    public List<PostDTO> getRegularPosts() {
        return postRepository.findByIsPinnedFalseOrderByCreatedAtDesc().stream()
                .map(p -> enrichAuthorDisplay(new PostDTO(p)))
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

        PostDTO dto = enrichAuthorDisplay(new PostDTO(post));

        // Poll 집계/상태 포함하여 내려주기
        pollRepository.findByPost_Id(postId).ifPresent(poll -> {
            java.util.List<PollOption> options = pollOptionRepository.findByPoll_Id(poll.getId());
            int[] votes = new int[options.size()];
            for (int i = 0; i < options.size(); i++) {
                votes[i] = (int) pollVoteRepository.countByPoll_IdAndOption_Id(poll.getId(), options.get(i).getId());
            }
            dto.setPollVotes(votes);
        });

        // 기본값
        dto.setHasUserVoted(null);
        dto.setUserVoteIndex(null);

        return dto;
    }

    // 게시글 생성
    @Transactional
    public PostDTO createPost(PostDTO postDTO) {
        Post post = new Post();
        post.setTitle(postDTO.getTitle());
        post.setContent(postDTO.getContent());
        post.setSummary(postDTO.getSummary());
        post.setCategory(postDTO.getCategory());
        // 작성자 식별자는 이메일을 사용 (이름 변경과 무관하게 고정)
        post.setAuthor(postDTO.getAuthor());
        post.setCreatedAt(java.time.LocalDateTime.now());
        post.setIsPinned(false);
        post.setViews(0);
        post.setLikes(0);
        
        Post savedPost = postRepository.save(post);

        // Save poll if provided: accept either explicit question or options-only
        boolean hasOptions = postDTO.getPollOptions() != null && !postDTO.getPollOptions().isEmpty();
        boolean hasQuestion = postDTO.getPollQuestion() != null && !postDTO.getPollQuestion().trim().isEmpty();
        if (hasQuestion || hasOptions) {
            Poll poll = new Poll();
            poll.setPost(savedPost);
            String question = hasQuestion ? postDTO.getPollQuestion().trim() : (postDTO.getTitle() != null ? postDTO.getTitle() : "Poll");
            poll.setQuestion(question);
            poll.setAllowMulti(Boolean.TRUE.equals(postDTO.getPollAllowMulti()));
            Poll savedPoll = pollRepository.save(poll);

            if (postDTO.getPollOptions() != null) {
                for (String opt : postDTO.getPollOptions()) {
                    if (opt == null || opt.trim().isEmpty()) continue;
                    PollOption option = new PollOption();
                    option.setPoll(savedPoll);
                    option.setOptionText(opt.trim());
                    option.setVoteCount(0);
                    pollOptionRepository.save(option);
                }
            }
        }

        return enrichAuthorDisplay(new PostDTO(postRepository.findById(savedPost.getId()).orElse(savedPost)));
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

    // ✅ Poll 투표 (간단 집계형: PollOption.voteCount 증가, 사용자 중복 투표 방지는 프런트/추가 구현으로 보강 가능)
    @Transactional
    public java.util.Map<String, Object> voteOnPoll(Long postId, String userEmail, int optionIndex) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        Poll poll = pollRepository.findByPost_Id(postId)
                .orElseThrow(() -> new RuntimeException("Poll not found for this post"));

        java.util.List<PollOption> options = pollOptionRepository.findByPoll_Id(poll.getId());
        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new IllegalArgumentException("Invalid option index");
        }

        // 사용자 1회 투표 제한
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean alreadyVoted = pollVoteRepository.findByPollAndUser(poll, user).isPresent();
        if (alreadyVoted) {
            throw new IllegalStateException("You have already voted in this poll.");
        }

        PollOption selected = options.get(optionIndex);
        pollVoteRepository.save(new com.slam.slam_backend.entity.PollVote(poll, selected, user));

        // vote_count는 집계형으로 즉시 반영
        selected.setVoteCount((selected.getVoteCount() == null ? 0 : selected.getVoteCount()) + 1);
        pollOptionRepository.save(selected);

        // 최신 집계 반환
        int[] pollVotes = new int[options.size()];
        for (int i = 0; i < options.size(); i++) {
            pollVotes[i] = (int) pollVoteRepository.countByPoll_IdAndOption_Id(poll.getId(), options.get(i).getId());
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("pollVotes", pollVotes);
        return result;
    }

    // ✅ 현재 사용자 투표 상태 조회 (hasUserVoted, userVoteIndex)
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getUserPollState(Long postId, String userEmail) {
        Poll poll = pollRepository.findByPost_Id(postId).orElse(null);
        if (poll == null) return null;
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) return null;
        return pollVoteRepository.findByPollAndUser(poll, user)
                .map(v -> {
                    java.util.List<PollOption> options = pollOptionRepository.findByPoll_Id(poll.getId());
                    Integer idx = null;
                    for (int i = 0; i < options.size(); i++) {
                        if (options.get(i).getId().equals(v.getOption().getId())) { idx = i; break; }
                    }
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("hasUserVoted", true);
                    map.put("userVoteIndex", idx);
                    return map;
                })
                .orElseGet(() -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("hasUserVoted", false);
                    map.put("userVoteIndex", null);
                    return map;
                });
    }

    // 댓글 추가
    @Transactional
    public CommentDTO addComment(Long postId, CommentDTO commentDTO, String authorEmail) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        Comment comment = new Comment();
        comment.setText(commentDTO.getText());
        // 작성자도 이메일로 저장
        comment.setAuthor(authorEmail != null ? authorEmail : commentDTO.getAuthor());
        comment.setCreatedAt(java.time.LocalDateTime.now());
        comment.setPost(post);
        
        Comment savedComment = commentRepository.save(comment);
        return enrichCommentAuthorDisplay(new CommentDTO(savedComment));
    }

    private PostDTO enrichAuthorDisplay(PostDTO dto) {
        if (dto == null) return null;
        String email = dto.getAuthorEmail() != null ? dto.getAuthorEmail() : dto.getAuthor();
        if (email != null) {
            userRepository.findByEmail(email).ifPresent(u -> {
                dto.setAuthorEmail(email);
                dto.setAuthorDisplayName(u.getName());
            });
        }
        return dto;
    }

    private CommentDTO enrichCommentAuthorDisplay(CommentDTO dto) {
        if (dto == null) return null;
        String email = dto.getAuthorEmail() != null ? dto.getAuthorEmail() : dto.getAuthor();
        if (email != null) {
            userRepository.findByEmail(email).ifPresent(u -> {
                dto.setAuthorEmail(email);
                dto.setAuthorDisplayName(u.getName());
            });
        }
        return dto;
    }

    // 게시글별 댓글 조회
    public List<CommentDTO> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId).stream()
                .map(c -> enrichCommentAuthorDisplay(new CommentDTO(c)))
                .collect(Collectors.toList());
    }

    // 게시글 삭제 (작성자 또는 관리자만 가능)
    @Transactional
    public boolean deletePost(Long postId, String userEmail) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 권한 확인: 작성자이거나 관리자인지 확인
        boolean isAuthorByEmail = post.getAuthor().equals(userEmail);
        boolean isAuthorByName = post.getAuthor().equals(user.getName());
        boolean isAdmin = "ADMIN".equals(user.getRole());
        
        if (isAuthorByEmail || isAuthorByName || isAdmin) {
            postRepository.delete(post);
            return true;
        } else {
            return false; // 권한 없음
        }
    }


} 