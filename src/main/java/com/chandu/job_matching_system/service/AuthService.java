
package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.AuthResponse;
import com.chandu.job_matching_system.dto.RegisterRequest;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists"
            );
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);

        // Store the hash, never the plain-text password.
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Do not accept a role from the registration request.
        user.setRole("CANDIDATE");

        User savedUser = userRepository.save(user);

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                "Registration successful"
        );
    }
}
