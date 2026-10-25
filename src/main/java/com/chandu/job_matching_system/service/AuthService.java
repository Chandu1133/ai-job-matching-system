package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.AuthResponse;
import com.chandu.job_matching_system.dto.LoginRequest;
import com.chandu.job_matching_system.dto.RegisterRequest;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));

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

    @Transactional
    public Authentication authenticateUser(LoginRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String rawPassword = request.getPassword();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            throw new BadCredentialsException("Invalid email or password");
        }

        boolean matches = false;
        if (storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            matches = passwordEncoder.matches(rawPassword, storedPassword)
                    || passwordEncoder.matches(rawPassword.trim(), storedPassword);
        } else {
            matches = storedPassword.equals(rawPassword)
                    || storedPassword.equals(rawPassword.trim());
            if (matches) {
                user.setPassword(passwordEncoder.encode(rawPassword.trim()));
                userRepository.save(user);
            }
        }

        if (!matches) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String role = user.getRole();
        if (role == null || role.isBlank()) {
            role = "CANDIDATE";
            user.setRole(role);
            userRepository.save(user);
        } else {
            role = role.trim().toUpperCase();
            if (role.startsWith("ROLE_")) {
                role = role.substring(5);
            }
        }

        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPassword(),
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    @Transactional
    public AuthResponse resetPassword(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No account found with this email. Please click Register first."
                ));

        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("CANDIDATE");
        }
        User saved = userRepository.save(user);

        return new AuthResponse(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getRole(),
                "Password updated and logged in successfully"
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse getAuthResponseByEmail(String email, String message) {

        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                message
        );
    }
}
