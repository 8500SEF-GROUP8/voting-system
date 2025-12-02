package com.votingsystem.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "votes")
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
    
    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VoteOption> options = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    private VoteStatus status = VoteStatus.DRAFT;
    
    @Enumerated(EnumType.STRING)
    private VotePermission permission = VotePermission.PUBLIC;
    
    private String shareToken;
    
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private LocalDateTime closedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (shareToken == null) {
            shareToken = java.util.UUID.randomUUID().toString();
        }
    }
    
    public enum VoteStatus {
        DRAFT, PUBLISHED, CLOSED, DELETED
    }
    
    public enum VotePermission {
        PUBLIC, PRIVATE, LINK_ONLY
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
    
    public User getCreator() {
        return creator;
    }
    
    public void setCreator(User creator) {
        this.creator = creator;
    }
    
    public List<VoteOption> getOptions() {
        return options;
    }
    
    public void setOptions(List<VoteOption> options) {
        this.options = options;
    }
    
    public VoteStatus getStatus() {
        return status;
    }
    
    public void setStatus(VoteStatus status) {
        this.status = status;
    }
    
    public VotePermission getPermission() {
        return permission;
    }
    
    public void setPermission(VotePermission permission) {
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
}




