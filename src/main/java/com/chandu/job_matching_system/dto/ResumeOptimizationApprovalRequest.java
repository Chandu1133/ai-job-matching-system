package com.chandu.job_matching_system.dto;

import jakarta.validation.constraints.Size;

public class ResumeOptimizationApprovalRequest {

    @Size(max = 20000, message = "Revised resume content cannot exceed 20000 characters")
    private String revisedResumePreview;

    private boolean applyToOriginalResume = false;

    public ResumeOptimizationApprovalRequest() {
    }

    public String getRevisedResumePreview() { return revisedResumePreview; }
    public void setRevisedResumePreview(String revisedResumePreview) { this.revisedResumePreview = revisedResumePreview; }

    public boolean isApplyToOriginalResume() { return applyToOriginalResume; }
    public void setApplyToOriginalResume(boolean applyToOriginalResume) { this.applyToOriginalResume = applyToOriginalResume; }
}
