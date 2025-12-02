package com.votingsystem.controller;

import com.votingsystem.dto.CreateVoteRequest;
import com.votingsystem.dto.VoteDTO;
import com.votingsystem.model.*;
import com.votingsystem.repository.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/votes")
@CrossOrigin(origins = "*")
public class VoteController {
    
    @Autowired
    private VoteRepository voteRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VoteOptionRepository voteOptionRepository;
    
    @Autowired
    private VoteResponseRepository voteResponseRepository;
    
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return (Long) authentication.getPrincipal();
    }
    
    @PostMapping
    @Transactional
    public ResponseEntity<?> createVote(@Valid @RequestBody CreateVoteRequest request, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            User creator = userRepository.findById(userId).orElse(null);
            if (creator == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "User not found. Please login again.");
                errorResponse.put("error", "Unauthorized");
                return ResponseEntity.status(401).body(errorResponse);
            }
            
            Vote vote = new Vote();
            vote.setTitle(request.getTitle());
            vote.setDescription(request.getDescription());
            vote.setCreator(creator);
            vote.setStatus(Vote.VoteStatus.DRAFT);
            
            if (request.getPermission() != null) {
                try {
                    vote.setPermission(Vote.VotePermission.valueOf(request.getPermission().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    vote.setPermission(Vote.VotePermission.PUBLIC);
                }
            }
            
            vote = voteRepository.save(vote);
            
            for (String optionText : request.getOptions()) {
                VoteOption option = new VoteOption();
                option.setText(optionText);
                option.setVote(vote);
                option = voteOptionRepository.save(option);
                vote.getOptions().add(option);
            }
            
            vote = voteRepository.save(vote);
            
            if (vote.getCreator() != null) {
                Long creatorId = vote.getCreator().getId();
                String creatorUsername = vote.getCreator().getUsername();
            }
            
            if (vote.getOptions() != null && !vote.getOptions().isEmpty()) {
                for (VoteOption opt : vote.getOptions()) {
                    Long optId = opt.getId();
                    String optText = opt.getText();
                    Integer optCount = opt.getVoteCount();
                }
            }
            
            VoteDTO dto = convertToDTO(vote, userId);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            e.printStackTrace(); // Log the exception
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error creating vote: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            errorResponse.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> editVote(@PathVariable Long id, @Valid @RequestBody CreateVoteRequest request, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Vote vote = voteRepository.findById(id).orElse(null);
        
        if (vote == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!vote.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(403).body("You can only edit your own votes");
        }
        
        if (vote.getStatus() == Vote.VoteStatus.PUBLISHED) {
            return ResponseEntity.badRequest().body("Cannot edit published vote");
        }
        
        if (vote.getStatus() == Vote.VoteStatus.DELETED) {
            return ResponseEntity.badRequest().body("Cannot edit deleted vote");
        }
        
        if (vote.getStatus() == Vote.VoteStatus.CLOSED) {
            return ResponseEntity.badRequest().body("Cannot edit closed vote");
        }
        
        vote.setTitle(request.getTitle());
        vote.setDescription(request.getDescription());
        
        if (request.getPermission() != null) {
            try {
                vote.setPermission(Vote.VotePermission.valueOf(request.getPermission().toUpperCase()));
            } catch (IllegalArgumentException e) {
                vote.setPermission(Vote.VotePermission.PUBLIC);
            }
        }
        
        vote.getOptions().clear();
        for (String optionText : request.getOptions()) {
            VoteOption option = new VoteOption();
            option.setText(optionText);
            option.setVote(vote);
            vote.getOptions().add(option);
        }
        
        voteRepository.save(vote);
        
        return ResponseEntity.ok(convertToDTO(vote, userId));
    }
    
    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishVote(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Vote vote = voteRepository.findById(id).orElse(null);
        
        if (vote == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!vote.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(403).body("You can only publish your own votes");
        }
        
        if (vote.getOptions().size() < 2) {
            return ResponseEntity.badRequest().body("Vote must have at least 2 options");
        }
        
        vote.setStatus(Vote.VoteStatus.PUBLISHED);
        vote.setPublishedAt(LocalDateTime.now());
        voteRepository.save(vote);
        
        return ResponseEntity.ok(convertToDTO(vote, userId));
    }
    
    @GetMapping
    public ResponseEntity<List<VoteDTO>> browseVotes(@RequestParam(required = false) String status, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<Vote> votes;
        
        if (status != null && !status.isEmpty()) {
            try {
                Vote.VoteStatus voteStatus = Vote.VoteStatus.valueOf(status.toUpperCase());
                votes = voteRepository.findByStatus(voteStatus);
            } catch (IllegalArgumentException e) {
                votes = voteRepository.findAll();
            }
        } else {
            votes = voteRepository.findAll();
        }
        
        votes = votes.stream()
            .filter(vote -> {
                if (vote.getStatus() != Vote.VoteStatus.PUBLISHED) {
                    return vote.getCreator().getId().equals(userId);
                }
                if (vote.getPermission() == Vote.VotePermission.PUBLIC) {
                    return true;
                }
                if (vote.getPermission() == Vote.VotePermission.PRIVATE) {
                    return vote.getCreator().getId().equals(userId);
                }
                return true;
            })
            .collect(Collectors.toList());
        
        List<VoteDTO> voteDTOs = votes.stream()
            .map(vote -> convertToDTO(vote, userId))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(voteDTOs);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<VoteDTO> getVote(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Vote vote = voteRepository.findById(id).orElse(null);
        
        if (vote == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (vote.getStatus() != Vote.VoteStatus.PUBLISHED && !vote.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        
        if (vote.getPermission() == Vote.VotePermission.PRIVATE && !vote.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(convertToDTO(vote, userId));
    }
    
    @PostMapping("/{id}/participate")
    public ResponseEntity<?> participateInVote(@PathVariable Long id, @RequestParam Long optionId, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Vote vote = voteRepository.findById(id).orElse(null);
        
        if (vote == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (vote.getStatus() != Vote.VoteStatus.PUBLISHED) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Vote is not published");
            errorResponse.put("error", "BadRequest");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        if (vote.getClosedAt() != null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Vote is closed");
            errorResponse.put("error", "BadRequest");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "User not found. Please login again.");
            errorResponse.put("error", "Unauthorized");
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        if (voteResponseRepository.existsByVoteAndUser(vote, user)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "You have already voted");
            errorResponse.put("error", "BadRequest");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        VoteOption option = voteOptionRepository.findById(optionId).orElse(null);
        if (option == null || !option.getVote().getId().equals(id)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid option");
            errorResponse.put("error", "BadRequest");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        VoteResponse response = new VoteResponse();
        response.setVote(vote);
        response.setUser(user);
        response.setSelectedOption(option);
        voteResponseRepository.save(response);
        
        option.setVoteCount(option.getVoteCount() + 1);
        voteOptionRepository.save(option);
        
        vote = voteRepository.findById(vote.getId()).orElse(vote);
        
        return ResponseEntity.ok(convertToDTO(vote, userId));
    }
    
    @GetMapping("/{id}/results")
    public ResponseEntity<VoteDTO> getVoteResults(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Vote vote = voteRepository.findById(id).orElse(null);
        
        if (vote == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(convertToDTO(vote, userId));
    }
    
    @GetMapping("/share/{token}")
    public ResponseEntity<?> getVoteByShareToken(@PathVariable String token) {
        Vote vote = voteRepository.findByShareToken(token).orElse(null);
        
        if (vote == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Vote not found with the provided share token");
            errorResponse.put("error", "NotFound");
            return ResponseEntity.status(404).body(errorResponse);
        }
        
        if (vote.getStatus() != Vote.VoteStatus.PUBLISHED) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "This vote is not published yet");
            errorResponse.put("error", "BadRequest");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        vote = voteRepository.findById(vote.getId()).orElse(vote);
        
        VoteDTO dto = convertToDTO(vote, null);
        return ResponseEntity.ok(dto);
    }
    
    @PostMapping("/{id}/participate-share")
    public ResponseEntity<?> participateInVoteByShare(@PathVariable Long id, @RequestParam Long optionId, @RequestParam(required = false) String token, Authentication authentication) {
        System.out.println("=== Participate by Share Debug ===");
        System.out.println("Vote ID: " + id);
        System.out.println("Option ID: " + optionId);
        System.out.println("Share Token: " + token);
        System.out.println("Authentication: " + authentication);
        if (authentication != null) {
            System.out.println("Is Authenticated: " + authentication.isAuthenticated());
            System.out.println("Principal: " + authentication.getPrincipal());
            System.out.println("Principal Type: " + (authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getName() : "null"));
        }
        System.out.println("================================");
        
        Vote vote = voteRepository.findById(id).orElse(null);
        
        if (vote == null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Vote not found");
            errorResponse.put("error", "NotFound");
            return ResponseEntity.status(404).body(errorResponse);
        }
        
        if (vote.getPermission() == Vote.VotePermission.LINK_ONLY && (token == null || !vote.getShareToken().equals(token))) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid share token");
            errorResponse.put("error", "Forbidden");
            return ResponseEntity.status(403).body(errorResponse);
        }
        
        if (vote.getStatus() != Vote.VoteStatus.PUBLISHED) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Vote is not published");
            errorResponse.put("error", "BadRequest");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        if (vote.getClosedAt() != null) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Vote is closed");
            errorResponse.put("error", "BadRequest");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        VoteOption option = voteOptionRepository.findById(optionId).orElse(null);
        if (option == null || !option.getVote().getId().equals(id)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid option");
            errorResponse.put("error", "BadRequest");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        User user = null;
        Long userId = null;
        
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof Long) {
                    userId = (Long) principal;
                } else if (principal != null && !"anonymousUser".equals(principal.toString())) {
                    try {
                        userId = Long.parseLong(principal.toString());
                    } catch (NumberFormatException e) {
                    }
                }
                
                if (userId != null) {
                    user = userRepository.findById(userId).orElse(null);
                    
                    if (user != null && voteResponseRepository.existsByVoteAndUser(vote, user)) {
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("message", "You have already voted");
                        errorResponse.put("error", "BadRequest");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking user authentication: " + e.getMessage());
        }
        
        VoteResponse response = new VoteResponse();
        response.setVote(vote);
        response.setUser(user);
        response.setSelectedOption(option);
        voteResponseRepository.save(response);
        
        option.setVoteCount(option.getVoteCount() + 1);
        voteOptionRepository.save(option);
        
        vote = voteRepository.findById(vote.getId()).orElse(vote);
        
        VoteDTO dto = convertToDTO(vote, userId);
        return ResponseEntity.ok(dto);
    }
    
    @PutMapping("/{id}/permission")
    public ResponseEntity<?> setVotePermission(@PathVariable Long id, @RequestParam String permission, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Vote vote = voteRepository.findById(id).orElse(null);
        
        if (vote == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!vote.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(403).body("You can only modify your own votes");
        }
        
        try {
            vote.setPermission(Vote.VotePermission.valueOf(permission.toUpperCase()));
            voteRepository.save(vote);
            return ResponseEntity.ok(convertToDTO(vote, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid permission type");
        }
    }
    
    @PostMapping("/{id}/close")
    public ResponseEntity<?> closeVote(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Vote vote = voteRepository.findById(id).orElse(null);
        
        if (vote == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!vote.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(403).body("You can only close your own votes");
        }
        
        vote.setStatus(Vote.VoteStatus.CLOSED);
        vote.setClosedAt(LocalDateTime.now());
        voteRepository.save(vote);
        
        return ResponseEntity.ok(convertToDTO(vote, userId));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVote(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Vote vote = voteRepository.findById(id).orElse(null);
        
        if (vote == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (!vote.getCreator().getId().equals(userId)) {
            return ResponseEntity.status(403).body("You can only delete your own votes");
        }
        
        vote.setStatus(Vote.VoteStatus.DELETED);
        voteRepository.save(vote);
        
        return ResponseEntity.ok().build();
    }
    
    private VoteDTO convertToDTO(Vote vote, Long currentUserId) {
        if (vote == null) {
            throw new IllegalArgumentException("Vote cannot be null");
        }
        
        VoteDTO dto = new VoteDTO();
        dto.setId(vote.getId());
        dto.setTitle(vote.getTitle());
        dto.setDescription(vote.getDescription());
        
        if (vote.getCreator() != null) {
            dto.setCreatorId(vote.getCreator().getId());
            dto.setCreatorUsername(vote.getCreator().getUsername() != null ? vote.getCreator().getUsername() : "Unknown");
        } else {
            dto.setCreatorId(null);
            dto.setCreatorUsername("Unknown");
        }
        
        dto.setStatus(vote.getStatus());
        dto.setPermission(vote.getPermission());
        dto.setShareToken(vote.getShareToken());
        dto.setCreatedAt(vote.getCreatedAt());
        dto.setPublishedAt(vote.getPublishedAt());
        dto.setClosedAt(vote.getClosedAt());
        
        int totalVotes = 0;
        if (vote.getOptions() != null) {
            totalVotes = vote.getOptions().stream()
                .mapToInt(option -> option.getVoteCount() != null ? option.getVoteCount() : 0)
                .sum();
        }
        dto.setTotalVotes(totalVotes);
        
        boolean hasVoted = false;
        if (currentUserId != null) {
            User user = userRepository.findById(currentUserId).orElse(null);
            if (user != null && vote != null) {
                hasVoted = voteResponseRepository.existsByVoteAndUser(vote, user);
            }
        }
        dto.setHasVoted(hasVoted);
        
        if (vote.getOptions() != null) {
            for (VoteOption option : vote.getOptions()) {
                VoteDTO.OptionDTO optionDTO = new VoteDTO.OptionDTO();
                optionDTO.setId(option.getId());
                optionDTO.setText(option.getText() != null ? option.getText() : "");
                optionDTO.setVoteCount(option.getVoteCount() != null ? option.getVoteCount() : 0);
                
                if (totalVotes > 0) {
                    optionDTO.setPercentage((double) optionDTO.getVoteCount() / totalVotes * 100);
                } else {
                    optionDTO.setPercentage(0.0);
                }
                
                dto.getOptions().add(optionDTO);
            }
        }
        
        return dto;
    }
}


