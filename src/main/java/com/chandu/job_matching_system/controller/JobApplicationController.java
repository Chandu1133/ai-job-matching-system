package com.chandu.job_matching_system.controller;

import com.chandu.job_matching_system.dto.*;
import com.chandu.job_matching_system.service.JobApplicationService;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationService applicationService;

    public JobApplicationController(JobApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/dashboard")
    public DashboardSummaryResponse getDashboardSummary(Authentication authentication) {
        return applicationService.getDashboardSummary(extractEmail(authentication));
    }

    @GetMapping("/assist/job/{jobId}")
    public ApplicationPrefillPreviewResponse getPrefillPreview(
            @PathVariable Long jobId,
            @RequestParam(required = false) Long resumeId,
            Authentication authentication) {

        return applicationService.generatePrefillPreview(
                jobId,
                resumeId,
                extractEmail(authentication)
        );
    }

    @PostMapping
    public ResponseEntity<JobApplicationResponse> createApplication(
            @Valid @RequestBody ApplicationCreateRequest request,
            Authentication authentication) {

        JobApplicationResponse created = applicationService.createApplication(
                request,
                extractEmail(authentication)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/confirm-submission")
    public ResponseEntity<JobApplicationResponse> confirmSubmission(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationSubmissionConfirmRequest request,
            Authentication authentication) {

        JobApplicationResponse confirmed = applicationService.confirmSubmission(
                id,
                request,
                extractEmail(authentication)
        );
        return ResponseEntity.ok(confirmed);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<JobApplicationResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationStatusUpdateRequest request,
            Authentication authentication) {

        JobApplicationResponse updated = applicationService.updateStatus(
                id,
                request,
                extractEmail(authentication)
        );
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public List<JobApplicationResponse> getMyApplications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {

        return applicationService.getMyApplications(
                status,
                fromDate,
                toDate,
                extractEmail(authentication)
        );
    }

    @GetMapping("/{id}")
    public JobApplicationResponse getApplicationById(
            @PathVariable Long id,
            Authentication authentication) {

        return applicationService.getApplicationById(id, extractEmail(authentication));
    }

    private String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
