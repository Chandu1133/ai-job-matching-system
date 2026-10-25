package com.chandu.job_matching_system.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class JobApplicationResponse {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private String company;
    private String location;
    private Long resumeId;
    private String status;
    private LocalDate applicationDate;
    private LocalDate followUpDate;
    private String jobSource;
    private String notes;
    private String prefilledDataPreview;
    private String submissionConfirmation;
    private Instant submittedAt;
    private List<String> statusHistory;
    private Instant createdAt;
    private Instant updatedAt;
    private String message;

    public JobApplicationResponse() {
    }

    public JobApplicationResponse(
            Long id,
            Long jobId,
            String jobTitle,
            String company,
            String location,
            Long resumeId,
            String status,
            LocalDate applicationDate,
            LocalDate followUpDate,
            String jobSource,
            String notes,
            String prefilledDataPreview,
            String submissionConfirmation,
            Instant submittedAt,
            List<String> statusHistory,
            Instant createdAt,
            Instant updatedAt,
            String message) {
        this.id = id;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.company = company;
        this.location = location;
        this.resumeId = resumeId;
        this.status = status;
        this.applicationDate = applicationDate;
        this.followUpDate = followUpDate;
        this.jobSource = jobSource;
        this.notes = notes;
        this.prefilledDataPreview = prefilledDataPreview;
        this.submissionConfirmation = submissionConfirmation;
        this.submittedAt = submittedAt;
        this.statusHistory = statusHistory;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDate applicationDate) { this.applicationDate = applicationDate; }

    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }

    public String getJobSource() { return jobSource; }
    public void setJobSource(String jobSource) { this.jobSource = jobSource; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPrefilledDataPreview() { return prefilledDataPreview; }
    public void setPrefilledDataPreview(String prefilledDataPreview) { this.prefilledDataPreview = prefilledDataPreview; }

    public String getSubmissionConfirmation() { return submissionConfirmation; }
    public void setSubmissionConfirmation(String submissionConfirmation) { this.submissionConfirmation = submissionConfirmation; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public List<String> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<String> statusHistory) { this.statusHistory = statusHistory; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
