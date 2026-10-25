package com.chandu.job_matching_system.controller;

import com.chandu.job_matching_system.dto.UserProfileResponse;
import com.chandu.job_matching_system.dto.UserProfileUpdateRequest;
import com.chandu.job_matching_system.service.UserProfileService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(
            Authentication authentication) {

        String email = getAuthenticatedEmail(authentication);
        return userProfileService.getProfileByEmail(email);
    }

    @PutMapping("/me")
    public UserProfileResponse updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UserProfileUpdateRequest request) {

        String email = getAuthenticatedEmail(authentication);
        return userProfileService.updateProfileByEmail(email, request);
    }

    private String getAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first"
            );
        }
        return authentication.getName();
    }
}
