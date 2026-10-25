package com.chandu.job_matching_system.dto;

import java.time.Instant;

public class ResumeResponse {

    private Long id;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String extractedText;
    private String extractedSkills;
    private String extractedEducation;
    private String extractedExperience;
    private String extractedProjects;
    private String extractionStatus;
    private boolean reviewedByCandidate;
    private Instant uploadedAt;
    private Instant updatedAt;
    private String message;

    public ResumeResponse() {
    }

    public ResumeResponse(
            Long id,
            String originalFileName,
            String fileType,
            Long fileSize,
            String extractedText,
            String extractedSkills,
            String extractedEducation,
            String extractedExperience,
            String extractedProjects,
            String extractionStatus,
            boolean reviewedByCandidate,
            Instant uploadedAt,
            Instant updatedAt,
            String message) {
        this.id = id;
        this.originalFileName = originalFileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.extractedText = extractedText;
        this.extractedSkills = extractedSkills;
        this.extractedEducation = extractedEducation;
        this.extractedExperience = extractedExperience;
        this.extractedProjects = extractedProjects;
        this.extractionStatus = extractionStatus;
        this.reviewedByCandidate = reviewedByCandidate;
        this.uploadedAt = uploadedAt;
        this.updatedAt = updatedAt;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getExtractedSkills() { return extractedSkills; }
    public void setExtractedSkills(String extractedSkills) { this.extractedSkills = extractedSkills; }

    public String getExtractedEducation() { return extractedEducation; }
    public void setExtractedEducation(String extractedEducation) { this.extractedEducation = extractedEducation; }

    public String getExtractedExperience() { return extractedExperience; }
    public void setExtractedExperience(String extractedExperience) { this.extractedExperience = extractedExperience; }

    public String getExtractedProjects() { return extractedProjects; }
    public void setExtractedProjects(String extractedProjects) { this.extractedProjects = extractedProjects; }

    public String getExtractionStatus() { return extractionStatus; }
    public void setExtractionStatus(String extractionStatus) { this.extractionStatus = extractionStatus; }

    public boolean isReviewedByCandidate() { return reviewedByCandidate; }
    public void setReviewedByCandidate(boolean reviewedByCandidate) { this.reviewedByCandidate = reviewedByCandidate; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
