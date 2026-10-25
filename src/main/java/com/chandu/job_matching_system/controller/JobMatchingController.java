package com.chandu.job_matching_system.controller;

import com.chandu.job_matching_system.dto.JobMatchResponse;
import com.chandu.job_matching_system.service.JobMatchingService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class JobMatchingController {

    private final JobMatchingService jobMatchingService;

    public JobMatchingController(JobMatchingService jobMatchingService) {
        this.jobMatchingService = jobMatchingService;
    }

    @PostMapping("/job/{jobId}")
    public ResponseEntity<JobMatchResponse> matchWithJob(
            @PathVariable Long jobId,
            @RequestParam(required = false) Long resumeId,
            Authentication authentication) {

        JobMatchResponse response = jobMatchingService.calculateAndSaveMatch(
                jobId,
                resumeId,
                extractEmail(authentication)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public List<JobMatchResponse> getMyMatches(Authentication authentication) {
        return jobMatchingService.getMyMatches(extractEmail(authentication));
    }

    @GetMapping("/job/{jobId}")
    public JobMatchResponse getMatchForJob(
            @PathVariable Long jobId,
            Authentication authentication) {

        return jobMatchingService.getMatchForJob(jobId, extractEmail(authentication));
    }

    private String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
