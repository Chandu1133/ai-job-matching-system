package com.chandu.job_matching_system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserProfileUpdateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String phone;

    @Size(max = 150, message = "Location cannot exceed 150 characters")
    private String location;

    @Size(max = 250, message = "Education cannot exceed 250 characters")
    private String education;

    @Size(max = 1000, message = "Skills cannot exceed 1000 characters")
    private String skills;

    @Size(max = 150, message = "Preferred role cannot exceed 150 characters")
    private String preferredJobRole;

    @Min(value = 0, message = "Experience cannot be negative")
    @Max(value = 60, message = "Experience cannot exceed 60 years")
    private Integer experienceYears;

    public UserProfileUpdateRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getPreferredJobRole() {
        return preferredJobRole;
    }

    public void setPreferredJobRole(String preferredJobRole) {
        this.preferredJobRole = preferredJobRole;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }
}