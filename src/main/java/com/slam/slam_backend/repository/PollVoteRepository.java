package com.slam.slam_backend.repository;

import com.slam.slam_backend.entity.Poll;
import com.slam.slam_backend.entity.PollVote;
import com.slam.slam_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    Optional<PollVote> findByPollAndUser(Poll poll, User user);
    long countByPoll_IdAndOption_Id(Long pollId, Long optionId);
}



