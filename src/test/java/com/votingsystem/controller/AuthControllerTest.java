package com.votingsystem.controller;

import com.votingsystem.dto.AuthResponse;
import com.votingsystem.dto.LoginRequest;
import com.votingsystem.dto.RegisterRequest;
import com.votingsystem.model.User;
import com.votingsystem.repository.UserRepository;
import com.votingsystem.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtUtil jwtUtil = new JwtUtil();

    @Test
    public void testRegister_Success() throws Exception {
        // Prepare data
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password");
        
        // Use reflection to inject the real JwtUtil instance
        Field jwtUtilField = AuthController.class.getDeclaredField("jwtUtil");
        jwtUtilField.setAccessible(true);
        jwtUtilField.set(authController, jwtUtil);

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hashedPassword");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Perform request
        ResponseEntity<?> responseEntity = authController.register(request);

        // Assert
        assertEquals(200, responseEntity.getStatusCode().value());
        AuthResponse authResponse = (AuthResponse) responseEntity.getBody();
        assertNotNull(authResponse);
        assertNotNull(authResponse.getToken());
        assertTrue(authResponse.getToken().length() > 0);
        assertEquals("testuser", authResponse.getUsername());
    }

    @Test
    public void testRegister_UsernameExists() {
        // Prepare data
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("test@example.com");
        request.setPassword("password");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Perform request
        ResponseEntity<?> responseEntity = authController.register(request);

        // Assert
        assertEquals(400, responseEntity.getStatusCode().value());
        AuthController.ErrorResponse errorResponse = (AuthController.ErrorResponse) responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals("Username already exists", errorResponse.getMessage());
    }

    @Test
    public void testRegister_EmailExists() {
        // Prepare data
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Perform request
        ResponseEntity<?> responseEntity = authController.register(request);

        // Assert
        assertEquals(400, responseEntity.getStatusCode().value());
        AuthController.ErrorResponse errorResponse = (AuthController.ErrorResponse) responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals("Email already exists", errorResponse.getMessage());
    }

    @Test
    public void testLogin_Success() throws Exception {
        // Prepare data
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("hashedPassword");

        // Use reflection to inject the real JwtUtil instance
        Field jwtUtilField = AuthController.class.getDeclaredField("jwtUtil");
        jwtUtilField.setAccessible(true);
        jwtUtilField.set(authController, jwtUtil);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);

        // Perform request
        ResponseEntity<?> responseEntity = authController.login(request);

        // Assert
        assertEquals(200, responseEntity.getStatusCode().value());
        AuthResponse authResponse = (AuthResponse) responseEntity.getBody();
        assertNotNull(authResponse);
        assertNotNull(authResponse.getToken());
        assertTrue(authResponse.getToken().length() > 0);
    }

    @Test
    public void testLogin_InvalidUsername() {
        // Prepare data
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistentuser");
        request.setPassword("password");

        when(userRepository.findByUsername("nonexistentuser")).thenReturn(Optional.empty());

        // Perform request
        ResponseEntity<?> responseEntity = authController.login(request);

        // Assert
        assertEquals(400, responseEntity.getStatusCode().value());
        AuthController.ErrorResponse errorResponse = (AuthController.ErrorResponse) responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals("Invalid username or password", errorResponse.getMessage());
    }

    @Test
    public void testLogin_InvalidPassword() {
        // Prepare data
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("hashedPassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        // Perform request
        ResponseEntity<?> responseEntity = authController.login(request);

        // Assert
        assertEquals(400, responseEntity.getStatusCode().value());
        AuthController.ErrorResponse errorResponse = (AuthController.ErrorResponse) responseEntity.getBody();
        assertNotNull(errorResponse);
        assertEquals("Invalid username or password", errorResponse.getMessage());
    }
}
