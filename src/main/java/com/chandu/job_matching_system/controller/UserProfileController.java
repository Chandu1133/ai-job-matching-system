package com.chandu.job_matching_system.controller;

import com.chandu.job_matching_system.dto.UserProfileResponse;
import com.chandu.job_matching_system.dto.UserProfileUpdateRequest;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.UserRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserRepository userRepository;

    public UserProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        return toResponse(user, "User profile retrieved successfully");
    }

    @PutMapping("/me")
    @Transactional
    public UserProfileResponse updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateRequest request) {

        User user = getAuthenticatedUser(authentication);

        user.setName(request.getName().trim());
        user.setPhone(trimToNull(request.getPhone()));
        user.setLocation(trimToNull(request.getLocation()));
        user.setEducation(trimToNull(request.getEducation()));
        user.setSkills(trimToNull(request.getSkills()));
        user.setPreferredJobRole(trimToNull(request.getPreferredJobRole()));
        user.setExperienceYears(request.getExperienceYears());

        User savedUser = userRepository.save(user);

        return toResponse(savedUser, "Profile updated successfully");
    }

    private User getAuthenticatedUser(Authentication authentication) {

        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first"
            );
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private UserProfileResponse toResponse(User user, String message) {

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.getLocation(),
                user.getEducation(),
                user.getSkills(),
                user.getPreferredJobRole(),
                user.getExperienceYears(),
                message
        );
    }
}