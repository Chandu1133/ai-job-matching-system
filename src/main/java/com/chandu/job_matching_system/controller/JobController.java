package com.chandu.job_matching_system.controller;

import com.chandu.job_matching_system.dto.JobPageResponse;
import com.chandu.job_matching_system.dto.JobRequest;
import com.chandu.job_matching_system.dto.JobResponse;
import com.chandu.job_matching_system.service.JobService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public JobPageResponse searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) Integer maxExperienceYears,
            @RequestParam(defaultValue = "false") boolean includeExpired,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "postedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        String email = extractEmail(authentication);
        return jobService.searchJobs(
                keyword,
                title,
                company,
                location,
                employmentType,
                maxExperienceYears,
                includeExpired,
                page,
                size,
                sortBy,
                sortDir,
                email
        );
    }

    @GetMapping("/bookmarks")
    public List<JobResponse> getBookmarkedJobs(Authentication authentication) {
        return jobService.getBookmarkedJobs(extractEmail(authentication));
    }

    @GetMapping("/{id}")
    public JobResponse getJobById(
            @PathVariable Long id,
            Authentication authentication) {

        return jobService.getJobById(id, extractEmail(authentication));
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody JobRequest request) {

        JobResponse created = jobService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/sync-external")
    public ResponseEntity<Map<String, Object>> syncExternalJobs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String apiKey) {

        Map<String, Object> result = jobService.syncExternalJobs(query, apiKey);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/bookmark")
    public ResponseEntity<JobResponse> bookmarkJob(
            @PathVariable Long id,
            Authentication authentication) {

        JobResponse response = jobService.bookmarkJob(id, extractEmail(authentication));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/bookmark")
    public ResponseEntity<Map<String, String>> removeBookmark(
            @PathVariable Long id,
            Authentication authentication) {

        jobService.removeBookmark(id, extractEmail(authentication));
        return ResponseEntity.ok(Map.of("message", "Job removed from bookmarks"));
    }

    private String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
