package com.chandu.job_matching_system.controller;

import com.chandu.job_matching_system.dto.ResumeOptimizationApprovalRequest;
import com.chandu.job_matching_system.dto.ResumeOptimizationResponse;
import com.chandu.job_matching_system.service.ResumeOptimizationService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/optimizations")
public class ResumeOptimizationController {

    private final ResumeOptimizationService optimizationService;

    public ResumeOptimizationController(ResumeOptimizationService optimizationService) {
        this.optimizationService = optimizationService;
    }

    @PostMapping("/resume/{resumeId}/job/{jobId}")
    public ResponseEntity<ResumeOptimizationResponse> generatePreview(
            @PathVariable Long resumeId,
            @PathVariable Long jobId,
            Authentication authentication) {

        ResumeOptimizationResponse response = optimizationService.generateOptimizationPreview(
                resumeId,
                jobId,
                extractEmail(authentication)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ResumeOptimizationResponse> approveOptimization(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ResumeOptimizationApprovalRequest request,
            Authentication authentication) {

        ResumeOptimizationResponse response = optimizationService.approveOptimization(
                id,
                request,
                extractEmail(authentication)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public List<ResumeOptimizationResponse> getMyOptimizations(Authentication authentication) {
        return optimizationService.getMyOptimizations(extractEmail(authentication));
    }

    @GetMapping("/{id}")
    public ResumeOptimizationResponse getOptimizationById(
            @PathVariable Long id,
            Authentication authentication) {

        return optimizationService.getOptimizationById(id, extractEmail(authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<java.util.Map<String, String>> deleteOptimization(
            @PathVariable Long id,
            Authentication authentication) {

        optimizationService.deleteOptimization(id, extractEmail(authentication));
        return ResponseEntity.ok(java.util.Map.of("message", "Optimized resume deleted successfully"));
    }

    private String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
