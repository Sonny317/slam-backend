package com.slam.slam_backend.service;

import com.slam.slam_backend.entity.Game;
import com.slam.slam_backend.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Service
public class GameService implements ApplicationRunner {

    @Autowired
    private GameRepository gameRepository;

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
}
