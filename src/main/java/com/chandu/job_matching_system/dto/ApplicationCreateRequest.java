package com.chandu.job_matching_system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ApplicationCreateRequest {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    private Long resumeId;

    @Size(max = 40, message = "Status cannot exceed 40 characters")
    private String status = "SAVED";

    private LocalDate followUpDate;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;

    public ApplicationCreateRequest() {
    }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
