package com.chandu.job_matching_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ApplicationSubmissionConfirmRequest {

    @NotBlank(message = "Submission confirmation details are required to mark an application as APPLIED")
    @Size(max = 500, message = "Confirmation details cannot exceed 500 characters")
    private String confirmationDetails;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;

    public ApplicationSubmissionConfirmRequest() {
    }

    public String getConfirmationDetails() { return confirmationDetails; }
    public void setConfirmationDetails(String confirmationDetails) { this.confirmationDetails = confirmationDetails; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
