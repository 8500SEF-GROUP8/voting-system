package com.votingsystem.repository;

import com.votingsystem.model.VoteResponse;
import com.votingsystem.model.Vote;
import com.votingsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface VoteResponseRepository extends JpaRepository<VoteResponse, Long> {
    Optional<VoteResponse> findByVoteAndUser(Vote vote, User user);
    List<VoteResponse> findByVote(Vote vote);
    boolean existsByVoteAndUser(Vote vote, User user);
}




