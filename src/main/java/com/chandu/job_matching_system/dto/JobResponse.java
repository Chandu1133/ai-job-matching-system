package com.chandu.job_matching_system.dto;

import java.time.LocalDate;

public class JobResponse {

    private Long id;
    private String title;
    private String company;
    private String location;
    private String employmentType;
    private Integer minExperienceYears;
    private Integer maxExperienceYears;
    private String salaryRange;
    private String description;
    private String requiredSkills;
    private String preferredQualifications;
    private String sourceUrl;
    private LocalDate postedDate;
    private LocalDate expiryDate;
    private boolean active;
    private boolean expired;
    private boolean seedData;
    private boolean bookmarked;

    public JobResponse() {
    }

    public JobResponse(
            Long id,
            String title,
            String company,
            String location,
            String employmentType,
            Integer minExperienceYears,
            Integer maxExperienceYears,
            String salaryRange,
            String description,
            String requiredSkills,
            String preferredQualifications,
            String sourceUrl,
            LocalDate postedDate,
            LocalDate expiryDate,
            boolean active,
            boolean expired,
            boolean seedData,
            boolean bookmarked) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.employmentType = employmentType;
        this.minExperienceYears = minExperienceYears;
        this.maxExperienceYears = maxExperienceYears;
        this.salaryRange = salaryRange;
        this.description = description;
        this.requiredSkills = requiredSkills;
        this.preferredQualifications = preferredQualifications;
        this.sourceUrl = sourceUrl;
        this.postedDate = postedDate;
        this.expiryDate = expiryDate;
        this.active = active;
        this.expired = expired;
        this.seedData = seedData;
        this.bookmarked = bookmarked;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isExpired() { return expired; }
    public void setExpired(boolean expired) { this.expired = expired; }

    public boolean isSeedData() { return seedData; }
    public void setSeedData(boolean seedData) { this.seedData = seedData; }

    public boolean isBookmarked() { return bookmarked; }
    public void setBookmarked(boolean bookmarked) { this.bookmarked = bookmarked; }
}
