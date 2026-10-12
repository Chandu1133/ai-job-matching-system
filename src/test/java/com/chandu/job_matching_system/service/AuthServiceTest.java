package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.AuthResponse;
import com.chandu.job_matching_system.dto.RegisterRequest;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void registerShouldSaveUserWithHashedPassword() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("testuser@example.com");
        request.setPassword("TestPass123!");

        when(userRepository.existsByEmail("testuser@example.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        AuthResponse response = authService.register(request);

        assertEquals(1L, response.getId());
        assertEquals("Test User", response.getName());
        assertEquals("testuser@example.com", response.getEmail());
        assertEquals("CANDIDATE", response.getRole());

        verify(userRepository).save(argThat(user ->
                passwordEncoder.matches(
                        "TestPass123!",
                        user.getPassword()
                )
                && !user.getPassword().equals("TestPass123!")
        ));
    }

    @Test
    void registerShouldRejectDuplicateEmail() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Another User");
        request.setEmail("testuser@example.com");
        request.setPassword("TestPass123!");

        when(userRepository.existsByEmail("testuser@example.com"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerShouldNormalizeEmail() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail(" TESTUSER@EXAMPLE.COM ");
        request.setPassword("TestPass123!");

        when(userRepository.existsByEmail("testuser@example.com"))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(2L);
                    return user;
                });

        AuthResponse response = authService.register(request);

        assertEquals("testuser@example.com", response.getEmail());
    }
}