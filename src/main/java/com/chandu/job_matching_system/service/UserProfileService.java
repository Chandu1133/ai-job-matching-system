package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.UserProfileResponse;
import com.chandu.job_matching_system.dto.UserProfileUpdateRequest;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByEmail(String email) {
        User user = findUserByEmail(email);
        return toResponse(user, "User profile retrieved successfully");
    }

    @Transactional
    public UserProfileResponse updateProfileByEmail(
            String email,
            UserProfileUpdateRequest request) {

        User user = findUserByEmail(email);

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

    private User findUserByEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        return userRepository.findByEmail(normalizedEmail)
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
