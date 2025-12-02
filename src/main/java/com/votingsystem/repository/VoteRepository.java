package com.votingsystem.repository;

import com.votingsystem.model.Vote;
import com.votingsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    List<Vote> findByCreator(User creator);
    List<Vote> findByStatus(Vote.VoteStatus status);
    Optional<Vote> findByShareToken(String shareToken);
    List<Vote> findByCreatorAndStatus(User creator, Vote.VoteStatus status);
}




