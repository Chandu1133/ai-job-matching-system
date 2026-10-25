package com.chandu.job_matching_system.dto;

import jakarta.validation.constraints.Size;

public class ResumeReviewRequest {

    @Size(max = 1000, message = "Skills cannot exceed 1000 characters")
    private String extractedSkills;

    @Size(max = 4000, message = "Education cannot exceed 4000 characters")
    private String extractedEducation;

    @Size(max = 6000, message = "Experience cannot exceed 6000 characters")
    private String extractedExperience;

    @Size(max = 4000, message = "Projects cannot exceed 4000 characters")
    private String extractedProjects;

    @Size(max = 20000, message = "Extracted text cannot exceed 20000 characters")
    private String extractedText;

    public ResumeReviewRequest() {
    }

    public String getExtractedSkills() { return extractedSkills; }
    public void setExtractedSkills(String extractedSkills) { this.extractedSkills = extractedSkills; }

    public String getExtractedEducation() { return extractedEducation; }
    public void setExtractedEducation(String extractedEducation) { this.extractedEducation = extractedEducation; }

    public String getExtractedExperience() { return extractedExperience; }
    public void setExtractedExperience(String extractedExperience) { this.extractedExperience = extractedExperience; }

    public String getExtractedProjects() { return extractedProjects; }
    public void setExtractedProjects(String extractedProjects) { this.extractedProjects = extractedProjects; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
}
