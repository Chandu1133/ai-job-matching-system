package com.chandu.job_matching_system.controller;

import com.chandu.job_matching_system.dto.AuthResponse;
import com.chandu.job_matching_system.dto.LoginRequest;
import com.chandu.job_matching_system.dto.RegisterRequest;
import com.chandu.job_matching_system.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    private final SessionAuthenticationStrategy sessionStrategy =
            new ChangeSessionIdAuthenticationStrategy();

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {

        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        Authentication authentication =
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken
                                .unauthenticated(
                                        request.getEmail().trim()
                                                .toLowerCase(),
                                        request.getPassword()
                                )
                );

        sessionStrategy.onAuthentication(
                authentication, httpRequest, httpResponse);

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context, httpRequest, httpResponse);

        return ResponseEntity.ok(
                new AuthResponse(
                        null,
                        authentication.getName(),
                        authentication.getName(),
                        authentication.getAuthorities()
                                .iterator().next().getAuthority(),
                        "Login successful"
                )
        );
    }
}