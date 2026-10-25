package com.chandu.job_matching_system.controller;

import com.chandu.job_matching_system.dto.ResumeResponse;
import com.chandu.job_matching_system.dto.ResumeReviewRequest;
import com.chandu.job_matching_system.service.ResumeService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> uploadResume(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        ResumeResponse response = resumeService.uploadResume(
                file,
                extractEmail(authentication)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ResumeResponse> getMyResumes(Authentication authentication) {
        return resumeService.getMyResumes(extractEmail(authentication));
    }

    @GetMapping("/{id}")
    public ResumeResponse getMyResumeById(
            @PathVariable Long id,
            Authentication authentication) {

        return resumeService.getMyResumeById(id, extractEmail(authentication));
    }

    @PutMapping("/{id}/review")
    public ResumeResponse reviewResume(
            @PathVariable Long id,
            @Valid @RequestBody ResumeReviewRequest request,
            Authentication authentication) {

        return resumeService.reviewAndCorrectResume(
                id,
                request,
                extractEmail(authentication)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteResume(
            @PathVariable Long id,
            Authentication authentication) {

        resumeService.deleteMyResume(id, extractEmail(authentication));
        return ResponseEntity.ok(Map.of("message", "Resume deleted successfully"));
    }

    private String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
