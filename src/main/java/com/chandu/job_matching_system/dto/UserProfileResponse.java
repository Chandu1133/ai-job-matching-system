package com.chandu.job_matching_system.dto;

public class UserProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String phone;
    private String location;
    private String education;
    private String skills;
    private String preferredJobRole;
    private Integer experienceYears;
    private String message;

    public UserProfileResponse() {
    }

    public UserProfileResponse(
            Long id,
            String name,
            String email,
            String role,
            String phone,
            String location,
            String education,
            String skills,
            String preferredJobRole,
            Integer experienceYears,
            String message) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.phone = phone;
        this.location = location;
        this.education = education;
        this.skills = skills;
        this.preferredJobRole = preferredJobRole;
        this.experienceYears = experienceYears;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getPreferredJobRole() { return preferredJobRole; }
    public void setPreferredJobRole(String preferredJobRole) {
        this.preferredJobRole = preferredJobRole;
    }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}