package com.votingsystem.model;

import jakarta.persistence.*;

@Entity
@Table(name = "vote_responses", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"vote_id", "user_id"})
})
public class VoteResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "option_id", nullable = false)
    private VoteOption selectedOption;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Vote getVote() {
        return vote;
    }
    
    public void setVote(Vote vote) {
        this.vote = vote;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public VoteOption getSelectedOption() {
        return selectedOption;
    }
    
    public void setSelectedOption(VoteOption selectedOption) {
        this.selectedOption = selectedOption;
    }
}




