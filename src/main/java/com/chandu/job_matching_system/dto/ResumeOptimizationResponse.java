package com.chandu.job_matching_system.dto;

import java.time.Instant;
import java.util.List;

public class ResumeOptimizationResponse {

    private Long id;
    private Long resumeId;
    private String originalFileName;
    private Long jobId;
    private String jobTitle;
    private String company;
    private List<String> supportedKeywords;
    private List<String> missingSkills;
    private List<String> suggestions;
    private String revisedResumePreview;
    private String status;
    private boolean approvedByCandidate;
    private Instant createdAt;
    private Instant approvedAt;
    private String message;

    public ResumeOptimizationResponse() {
    }

    public ResumeOptimizationResponse(
            Long id,
            Long resumeId,
            String originalFileName,
            Long jobId,
            String jobTitle,
            String company,
            List<String> supportedKeywords,
            List<String> missingSkills,
            List<String> suggestions,
            String revisedResumePreview,
            String status,
            boolean approvedByCandidate,
            Instant createdAt,
            Instant approvedAt,
            String message) {
        this.id = id;
        this.resumeId = resumeId;
        this.originalFileName = originalFileName;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.company = company;
        this.supportedKeywords = supportedKeywords;
        this.missingSkills = missingSkills;
        this.suggestions = suggestions;
        this.revisedResumePreview = revisedResumePreview;
        this.status = status;
        this.approvedByCandidate = approvedByCandidate;
        this.createdAt = createdAt;
        this.approvedAt = approvedAt;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public List<String> getSupportedKeywords() { return supportedKeywords; }
    public void setSupportedKeywords(List<String> supportedKeywords) { this.supportedKeywords = supportedKeywords; }

    public List<String> getMissingSkills() { return missingSkills; }
    public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    public String getRevisedResumePreview() { return revisedResumePreview; }
    public void setRevisedResumePreview(String revisedResumePreview) { this.revisedResumePreview = revisedResumePreview; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isApprovedByCandidate() { return approvedByCandidate; }
    public void setApprovedByCandidate(boolean approvedByCandidate) { this.approvedByCandidate = approvedByCandidate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
