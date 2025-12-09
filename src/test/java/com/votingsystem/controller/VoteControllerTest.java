package com.votingsystem.controller;

import com.votingsystem.dto.CreateVoteRequest;
import com.votingsystem.dto.VoteDTO;
import com.votingsystem.model.User;
import com.votingsystem.model.Vote;
import com.votingsystem.model.VoteOption;
import com.votingsystem.repository.UserRepository;
import com.votingsystem.repository.VoteOptionRepository;
import com.votingsystem.repository.VoteRepository;
import com.votingsystem.repository.VoteResponseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VoteControllerTest {

    @InjectMocks
    private VoteController voteController;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VoteOptionRepository voteOptionRepository;

    @Mock
    private VoteResponseRepository voteResponseRepository;

    @Test
    public void testCreateVote_Success() {
        // Prepare data
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        CreateVoteRequest request = new CreateVoteRequest();
        request.setTitle("Test Vote");
        request.setDescription("Test Description");
        request.setOptions(Arrays.asList("Option 1", "Option 2"));
        request.setPermission("PUBLIC");
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Vote vote = new Vote();
        vote.setId(1L);
        vote.setTitle(request.getTitle());
        vote.setCreator(user);
        vote.setStatus(Vote.VoteStatus.DRAFT);
        
        VoteOption option1 = new VoteOption();
        option1.setId(1L);
        option1.setText("Option 1");
        option1.setVote(vote);
        
        VoteOption option2 = new VoteOption();
        option2.setId(2L);
        option2.setText("Option 2");
        option2.setVote(vote);

        when(voteRepository.save(any(Vote.class))).thenReturn(vote);
        when(voteOptionRepository.save(any(VoteOption.class))).thenReturn(option1, option2);
        when(voteResponseRepository.existsByVoteAndUser(any(Vote.class), any(User.class))).thenReturn(false);

        // Perform request
        ResponseEntity<?> responseEntity = voteController.createVote(request, authentication);

        // Assert
        assertEquals(200, responseEntity.getStatusCode().value());
        VoteDTO voteDTO = (VoteDTO) responseEntity.getBody();
        assertNotNull(voteDTO);
        assertEquals("Test Vote", voteDTO.getTitle());
        assertEquals("testuser", voteDTO.getCreatorUsername());
    }

    @Test
    public void testEditVote_Success() {
        // Prepare data
        Long userId = 1L;
        Long voteId = 1L;
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        Vote vote = new Vote();
        vote.setId(voteId);
        vote.setTitle("Old Title");
        vote.setDescription("Old Description");
        vote.setCreator(user);
        vote.setStatus(Vote.VoteStatus.DRAFT);

        CreateVoteRequest request = new CreateVoteRequest();
        request.setTitle("New Title");
        request.setDescription("New Description");
        request.setOptions(Arrays.asList("Option A", "Option B"));
        request.setPermission("PRIVATE");

        Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null);

        when(voteRepository.findById(voteId)).thenReturn(Optional.of(vote));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user)); // for convertToDTO

        // Perform request
        ResponseEntity<?> responseEntity = voteController.editVote(voteId, request, authentication);

        // Assert
        assertEquals(200, responseEntity.getStatusCode().value());
        VoteDTO voteDTO = (VoteDTO) responseEntity.getBody();
        assertNotNull(voteDTO);
        assertEquals("New Title", voteDTO.getTitle());
        assertEquals("New Description", voteDTO.getDescription());
    }

    @Test
    public void testPublishVote_Success() {
        // Prepare data
        Long userId = 1L;
        Long voteId = 1L;
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        Vote vote = new Vote();
        vote.setId(voteId);
        vote.setCreator(user);
        vote.setStatus(Vote.VoteStatus.DRAFT);
        
        List<VoteOption> options = new ArrayList<>();
        options.add(new VoteOption());
        options.add(new VoteOption());
        vote.setOptions(options);

        Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null);

        when(voteRepository.findById(voteId)).thenReturn(Optional.of(vote));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user)); // for convertToDTO

        // Perform request
        ResponseEntity<?> responseEntity = voteController.publishVote(voteId, authentication);

        // Assert
        assertEquals(200, responseEntity.getStatusCode().value());
        VoteDTO voteDTO = (VoteDTO) responseEntity.getBody();
        assertNotNull(voteDTO);
        assertEquals(Vote.VoteStatus.PUBLISHED, voteDTO.getStatus());
    }

    @Test
    public void testGetVote_Success() {
        // Prepare data
        Long userId = 1L;
        Long voteId = 1L;
        User user = new User();
        user.setId(userId);

        Vote vote = new Vote();
        vote.setId(voteId);
        vote.setCreator(user);
        vote.setStatus(Vote.VoteStatus.PUBLISHED);

        Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null);

        when(voteRepository.findById(voteId)).thenReturn(Optional.of(vote));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Perform request
        ResponseEntity<VoteDTO> responseEntity = voteController.getVote(voteId, authentication);

        // Assert
        assertEquals(200, responseEntity.getStatusCode().value());
        VoteDTO voteDTO = responseEntity.getBody();
        assertNotNull(voteDTO);
        assertEquals(voteId, voteDTO.getId());
    }

    @Test
    public void testParticipateInVote_Success() {
        // Prepare data
        Long userId = 1L;
        Long voteId = 1L;
        Long optionId = 1L;

        User user = new User();
        user.setId(userId);

        Vote vote = new Vote();
        vote.setId(voteId);
        vote.setStatus(Vote.VoteStatus.PUBLISHED);

        VoteOption option = new VoteOption();
        option.setId(optionId);
        option.setVote(vote);
        option.setVoteCount(0);
        
        vote.setOptions(Arrays.asList(option));

        Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null);

        when(voteRepository.findById(voteId)).thenReturn(Optional.of(vote));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(voteResponseRepository.existsByVoteAndUser(vote, user)).thenReturn(false);
        when(voteOptionRepository.findById(optionId)).thenReturn(Optional.of(option));

        // Perform request
        ResponseEntity<?> responseEntity = voteController.participateInVote(voteId, optionId, authentication);

        // Assert
        assertEquals(200, responseEntity.getStatusCode().value());
        VoteDTO voteDTO = (VoteDTO) responseEntity.getBody();
        assertNotNull(voteDTO);
        assertEquals(1, voteDTO.getTotalVotes());
    }
}
