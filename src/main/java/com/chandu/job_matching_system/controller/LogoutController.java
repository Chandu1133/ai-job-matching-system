package com.chandu.job_matching_system.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LogoutController {

    private final SecurityContextLogoutHandler logoutHandler =
            new SecurityContextLogoutHandler();

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        logoutHandler.logout(request, response, authentication);

        return ResponseEntity.ok(
                Map.of("message", "Logout successful")
        );
    }
}