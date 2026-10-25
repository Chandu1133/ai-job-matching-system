package com.chandu.job_matching_system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class JobRequest {

    @NotBlank(message = "Job title is required")
    @Size(max = 150, message = "Job title cannot exceed 150 characters")
    private String title;

    @NotBlank(message = "Company name is required")
    @Size(max = 150, message = "Company name cannot exceed 150 characters")
    private String company;

    @NotBlank(message = "Location is required")
    @Size(max = 150, message = "Location cannot exceed 150 characters")
    private String location;

    @NotBlank(message = "Employment type is required")
    @Size(max = 50, message = "Employment type cannot exceed 50 characters")
    private String employmentType = "FULL_TIME";

    @Min(value = 0, message = "Minimum experience cannot be negative")
    private Integer minExperienceYears = 0;

    @Min(value = 0, message = "Maximum experience cannot be negative")
    private Integer maxExperienceYears;

    @Size(max = 100, message = "Salary range cannot exceed 100 characters")
    private String salaryRange;

    @NotBlank(message = "Job description is required")
    @Size(max = 10000, message = "Job description cannot exceed 10000 characters")
    private String description;

    @NotBlank(message = "Required skills are required")
    @Size(max = 1000, message = "Required skills cannot exceed 1000 characters")
    private String requiredSkills;

    @Size(max = 1000, message = "Preferred qualifications cannot exceed 1000 characters")
    private String preferredQualifications;

    @Size(max = 500, message = "Source URL cannot exceed 500 characters")
    private String sourceUrl;

    private LocalDate postedDate;

    private LocalDate expiryDate;

    private Boolean active = true;

    public JobRequest() {
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public Integer getMinExperienceYears() { return minExperienceYears; }
    public void setMinExperienceYears(Integer minExperienceYears) { this.minExperienceYears = minExperienceYears; }

    public Integer getMaxExperienceYears() { return maxExperienceYears; }
    public void setMaxExperienceYears(Integer maxExperienceYears) { this.maxExperienceYears = maxExperienceYears; }

    public String getSalaryRange() { return salaryRange; }
    public void setSalaryRange(String salaryRange) { this.salaryRange = salaryRange; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }

    public String getPreferredQualifications() { return preferredQualifications; }
    public void setPreferredQualifications(String preferredQualifications) { this.preferredQualifications = preferredQualifications; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public LocalDate getPostedDate() { return postedDate; }
    public void setPostedDate(LocalDate postedDate) { this.postedDate = postedDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
