package com.votingsystem.model;

import jakarta.persistence.*;

@Entity
@Table(name = "vote_options")
public class VoteOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String text;
    
    @ManyToOne
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;
    
    private Integer voteCount = 0;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public Vote getVote() {
        return vote;
    }
    
    public void setVote(Vote vote) {
        this.vote = vote;
    }
    
    public Integer getVoteCount() {
        return voteCount;
    }
    
    public void setVoteCount(Integer voteCount) {
        this.voteCount = voteCount;
    }
}




