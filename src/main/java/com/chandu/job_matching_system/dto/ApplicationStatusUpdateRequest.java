package com.chandu.job_matching_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ApplicationStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    @Size(max = 40, message = "Status cannot exceed 40 characters")
    private String status;

    private LocalDate followUpDate;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;

    @Size(max = 500, message = "Submission confirmation cannot exceed 500 characters")
    private String submissionConfirmation;

    public ApplicationStatusUpdateRequest() {
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getSubmissionConfirmation() { return submissionConfirmation; }
    public void setSubmissionConfirmation(String submissionConfirmation) { this.submissionConfirmation = submissionConfirmation; }
}
