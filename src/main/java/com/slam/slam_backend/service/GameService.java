package com.slam.slam_backend.service;

import com.slam.slam_backend.dto.GameCreateRequest;
import com.slam.slam_backend.dto.GameFeedbackCreateRequest;
import com.slam.slam_backend.entity.Game;
import com.slam.slam_backend.entity.GameFeedback;
import com.slam.slam_backend.entity.EventGame;
import com.slam.slam_backend.repository.GameRepository;
import com.slam.slam_backend.repository.GameFeedbackRepository;
import com.slam.slam_backend.repository.EventGameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GameService implements ApplicationRunner {

    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private GameFeedbackRepository gameFeedbackRepository;
    
    @Autowired
    private EventGameRepository eventGameRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 기본 게임 데이터 초기화 (중복 방지)
        initializeDefaultGames();
    }

    private void initializeDefaultGames() {
        // 기존 게임 개수 확인
        if (gameRepository.count() > 0) {
            return; // 이미 게임이 있으면 스킵
        }

        // 기본 게임들 추가
        Game[] defaultGames = {
            new Game("game-01", "Human Bingo", "Icebreaker game where players find people matching descriptions", "Icebreaker"),
            new Game("game-02", "Two Truths One Lie", "Players share three statements about themselves, others guess the lie", "Icebreaker"),
            new Game("game-03", "Scavenger Hunt", "Team-based treasure hunting activity", "Team Building"),
            new Game("game-04", "PET KINGDOM", "Interactive storytelling game about pets", "Creative"),
            new Game("game-05", "Write Your Letter", "Creative writing activity", "Creative"),
            new Game("game-06", "AI Art Guessing Game", "Guess artwork created by AI", "Entertainment"),
            new Game("game-07", "Vera's Battle", "Strategy card game", "Strategy"),
            new Game("game-08", "Bridge Jump Jump", "Physical coordination game", "Physical"),
            new Game("game-09", "Tempo", "Music and rhythm-based game", "Music"),
            new Game("game-10", "Speed Networking", "Quick conversation rounds", "Social"),
            new Game("game-11", "Charades", "Classic acting and guessing game", "Entertainment"),
            new Game("game-12", "Name That Tune", "Music guessing game", "Music")
        };

        for (Game game : defaultGames) {
            gameRepository.save(game);
        }

        System.out.println("Initialized " + defaultGames.length + " default games");
    }

    // ===== 게임 관리 API =====
    
    public List<Game> getAllActiveGames() {
        return gameRepository.findByActiveTrue();
    }

    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    public List<Game> getGamesByCategory(String category) {
        return gameRepository.findByCategoryAndActiveTrue(category);
    }

    public Game getGameByGameId(String gameId) {
        return gameRepository.findByGameId(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다: " + gameId));
    }

    /**
     * 새 게임 생성
     */
    @Transactional
    public Game createGame(GameCreateRequest request) {
        // 고유한 gameId 생성
        String gameId = generateUniqueGameId();

        Game game = new Game(
                gameId,
                request.getName(),
                request.getDescription(),
                request.getInstructions(),
                request.getExampleVideoUrl(),
                request.getCategory(),
                request.getMinParticipants(),
                request.getMaxParticipants(),
                request.getEstimatedDuration(),
                request.getDifficulty()
        );

        if (request.getActive() != null) {
            game.setActive(request.getActive());
        }
        
        return gameRepository.save(game);
    }

    /**
     * 게임 정보 수정
     */
    @Transactional
    public Game updateGame(String gameId, GameCreateRequest request) {
        Game game = getGameByGameId(gameId);
        
        game.setName(request.getName());
        game.setDescription(request.getDescription());
        game.setInstructions(request.getInstructions());
        game.setExampleVideoUrl(request.getExampleVideoUrl());
        game.setCategory(request.getCategory());
        game.setMinParticipants(request.getMinParticipants());
        game.setMaxParticipants(request.getMaxParticipants());
        game.setEstimatedDuration(request.getEstimatedDuration());
        game.setDifficulty(request.getDifficulty());
        
        if (request.getActive() != null) {
            game.setActive(request.getActive());
        }
        
        return gameRepository.save(game);
    }

    /**
     * 게임 활성화/비활성화
     */
    @Transactional
    public Game toggleGameActive(String gameId) {
        Game game = getGameByGameId(gameId);
        game.setActive(!game.isActive());
        return gameRepository.save(game);
    }

    /**
     * 게임 피드백 생성
     */
    @Transactional
    public GameFeedback createGameFeedback(GameFeedbackCreateRequest request, String submittedBy) {
        // 게임 존재 여부 확인
        getGameByGameId(request.getGameId());

        GameFeedback feedback = GameFeedback.builder()
                .eventId(request.getEventId())
                .gameId(request.getGameId())
                .rating(request.getRating())
                .engagement(request.getEngagement())
                .difficulty(request.getDifficulty())
                .comment(request.getComment())
                .actualParticipants(request.getActualParticipants())
                .actualDuration(request.getActualDuration())
                .organizerNotes(request.getOrganizerNotes())
                .submittedBy(submittedBy)
                .build();

        return gameFeedbackRepository.save(feedback);
    }

    /**
     * 특정 이벤트의 게임 피드백 조회
     */
    public List<GameFeedback> getGameFeedbacksByEvent(Long eventId) {
        return gameFeedbackRepository.findByEventId(eventId);
    }

    /**
     * 특정 게임의 모든 피드백 조회
     */
    public List<GameFeedback> getGameFeedbacksByGame(String gameId) {
        return gameFeedbackRepository.findByGameIdOrderByCreatedAtDesc(gameId);
    }

    /**
     * 특정 이벤트-게임 조합의 피드백 조회
     */
    public List<GameFeedback> getGameFeedbacksByEventAndGame(Long eventId, String gameId) {
        return gameFeedbackRepository.findByEventIdAndGameId(eventId, gameId);
    }

    /**
     * 특정 이벤트에 할당된 게임들 조회
     */
    public List<Game> getGamesByEventId(Long eventId) {
        // EventGame 엔티티를 통해 이벤트에 할당된 게임들을 조회
        List<EventGame> eventGames = eventGameRepository.findByEventId(eventId);
        List<String> gameIds = eventGames.stream()
                .map(EventGame::getGameId)
                .toList();
        
        if (gameIds.isEmpty()) {
            return List.of(); // 할당된 게임이 없으면 빈 리스트 반환
        }
        
        return gameRepository.findByGameIdIn(gameIds);
    }
    
    /**
     * 이벤트에 게임들 할당
     */
    @Transactional
    public void assignGamesToEvent(Long eventId, List<String> gameIds) {
        // 기존 할당된 게임들 삭제
        eventGameRepository.deleteByEventId(eventId);
        
        // 새로운 게임들 할당
        for (String gameId : gameIds) {
            EventGame eventGame = new EventGame(eventId, gameId);
            eventGameRepository.save(eventGame);
        }
        
        System.out.println("Assigned games to event: " + eventId + ", games: " + gameIds);
    }
    
    /**
     * 고유한 게임 ID 생성 
     */
    private String generateUniqueGameId() {
        String gameId;
        do {
            gameId = "game-" + UUID.randomUUID().toString().substring(0, 8);
        } while (gameRepository.findByGameId(gameId).isPresent());
        
        return gameId;
    }
}
