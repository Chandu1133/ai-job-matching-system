package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.UserProfileResponse;
import com.chandu.job_matching_system.dto.UserProfileUpdateRequest;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(userRepository);
    }

    @Test
    void getProfileByEmailShouldReturnUserProfile() {
        User user = new User();
        user.setId(10L);
        user.setName("Chandu");
        user.setEmail("chandu@example.com");
        user.setRole("CANDIDATE");
        user.setSkills("Java, Spring Boot, MySQL");
        user.setExperienceYears(2);

        when(userRepository.findByEmail("chandu@example.com"))
                .thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.getProfileByEmail("chandu@example.com");

        assertEquals(10L, response.getId());
        assertEquals("Chandu", response.getName());
        assertEquals("chandu@example.com", response.getEmail());
        assertEquals("Java, Spring Boot, MySQL", response.getSkills());
        assertEquals(2, response.getExperienceYears());
        assertEquals("User profile retrieved successfully", response.getMessage());
    }

    @Test
    void updateProfileByEmailShouldTrimFieldsAndPersist() {
        User user = new User();
        user.setId(10L);
        user.setName("Old Name");
        user.setEmail("chandu@example.com");
        user.setRole("CANDIDATE");

        when(userRepository.findByEmail("chandu@example.com"))
                .thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setName("  Chandu Updated  ");
        request.setPhone("   ");
        request.setLocation(" Hyderabad ");
        request.setEducation(" B.Tech CSE ");
        request.setSkills(" Java, Spring Boot, React ");
        request.setPreferredJobRole(" Backend Developer ");
        request.setExperienceYears(3);

        UserProfileResponse response = userProfileService.updateProfileByEmail(
                "chandu@example.com",
                request
        );

        assertEquals("Chandu Updated", response.getName());
        assertNull(response.getPhone());
        assertEquals("Hyderabad", response.getLocation());
        assertEquals("B.Tech CSE", response.getEducation());
        assertEquals("Java, Spring Boot, React", response.getSkills());
        assertEquals("Backend Developer", response.getPreferredJobRole());
        assertEquals(3, response.getExperienceYears());
        assertEquals("Profile updated successfully", response.getMessage());
    }

    @Test
    void getProfileByEmailShouldThrowNotFoundWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userProfileService.getProfileByEmail("missing@example.com")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
