package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByActiveTrue();
    Optional<Game> findByGameId(String gameId);
    List<Game> findByCategoryAndActiveTrue(String category);
    List<Game> findByGameIdIn(List<String> gameIds);
}
