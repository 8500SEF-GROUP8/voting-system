package com.votingsystem.dto;

import com.votingsystem.model.Vote;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VoteDTO {
    private Long id;
    private String title;
    private String description;
    private Long creatorId;
    private String creatorUsername;
    private List<OptionDTO> options = new ArrayList<>();
    private Vote.VoteStatus status;
    private Vote.VotePermission permission;
    private String shareToken;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private LocalDateTime closedAt;
    private boolean hasVoted;
    private Integer totalVotes;
    
    public static class OptionDTO {
        private Long id;
        private String text;
        private Integer voteCount;
        private Double percentage;
        
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
        
        public Integer getVoteCount() {
            return voteCount;
        }
        
        public void setVoteCount(Integer voteCount) {
            this.voteCount = voteCount;
        }
        
        public Double getPercentage() {
            return percentage;
        }
        
        public void setPercentage(Double percentage) {
            this.percentage = percentage;
        }
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Long getCreatorId() {
        return creatorId;
    }
    
    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }
    
    public String getCreatorUsername() {
        return creatorUsername;
    }
    
    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }
    
    public List<OptionDTO> getOptions() {
        return options;
    }
    
    public void setOptions(List<OptionDTO> options) {
        this.options = options;
    }
    
    public Vote.VoteStatus getStatus() {
        return status;
    }
    
    public void setStatus(Vote.VoteStatus status) {
        this.status = status;
    }
    
    public Vote.VotePermission getPermission() {
        return permission;
    }
    
    public void setPermission(Vote.VotePermission permission) {
        this.permission = permission;
    }
    
    public String getShareToken() {
        return shareToken;
    }
    
    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
    
    public LocalDateTime getClosedAt() {
        return closedAt;
    }
    
    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
    
    public boolean isHasVoted() {
        return hasVoted;
    }
    
    public void setHasVoted(boolean hasVoted) {
        this.hasVoted = hasVoted;
    }
    
    public Integer getTotalVotes() {
        return totalVotes;
    }
    
    public void setTotalVotes(Integer totalVotes) {
        this.totalVotes = totalVotes;
    }
}




