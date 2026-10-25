package com.chandu.job_matching_system.dto;

public class ApplicationPrefillPreviewResponse {

    private Long jobId;
    private String jobTitle;
    private String company;
    private String directApplicationUrl;
    private boolean automatedSubmissionSupported;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String candidateLocation;
    private String candidateEducation;
    private String verifiedSkills;
    private Integer experienceYears;
    private Long selectedResumeId;
    private String selectedResumeFileName;
    private String tailoredCoverNote;
    private String workflowGuidance;

    public ApplicationPrefillPreviewResponse() {
    }

    public ApplicationPrefillPreviewResponse(
            Long jobId,
            String jobTitle,
            String company,
            String directApplicationUrl,
            boolean automatedSubmissionSupported,
            String candidateName,
            String candidateEmail,
            String candidatePhone,
            String candidateLocation,
            String candidateEducation,
            String verifiedSkills,
            Integer experienceYears,
            Long selectedResumeId,
            String selectedResumeFileName,
            String tailoredCoverNote,
            String workflowGuidance) {
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.company = company;
        this.directApplicationUrl = directApplicationUrl;
        this.automatedSubmissionSupported = automatedSubmissionSupported;
        this.candidateName = candidateName;
        this.candidateEmail = candidateEmail;
        this.candidatePhone = candidatePhone;
        this.candidateLocation = candidateLocation;
        this.candidateEducation = candidateEducation;
        this.verifiedSkills = verifiedSkills;
        this.experienceYears = experienceYears;
        this.selectedResumeId = selectedResumeId;
        this.selectedResumeFileName = selectedResumeFileName;
        this.tailoredCoverNote = tailoredCoverNote;
        this.workflowGuidance = workflowGuidance;
    }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getDirectApplicationUrl() { return directApplicationUrl; }
    public void setDirectApplicationUrl(String directApplicationUrl) { this.directApplicationUrl = directApplicationUrl; }

    public boolean isAutomatedSubmissionSupported() { return automatedSubmissionSupported; }
    public void setAutomatedSubmissionSupported(boolean automatedSubmissionSupported) { this.automatedSubmissionSupported = automatedSubmissionSupported; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }

    public String getCandidatePhone() { return candidatePhone; }
    public void setCandidatePhone(String candidatePhone) { this.candidatePhone = candidatePhone; }

    public String getCandidateLocation() { return candidateLocation; }
    public void setCandidateLocation(String candidateLocation) { this.candidateLocation = candidateLocation; }

    public String getCandidateEducation() { return candidateEducation; }
    public void setCandidateEducation(String candidateEducation) { this.candidateEducation = candidateEducation; }

    public String getVerifiedSkills() { return verifiedSkills; }
    public void setVerifiedSkills(String verifiedSkills) { this.verifiedSkills = verifiedSkills; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public Long getSelectedResumeId() { return selectedResumeId; }
    public void setSelectedResumeId(Long selectedResumeId) { this.selectedResumeId = selectedResumeId; }

    public String getSelectedResumeFileName() { return selectedResumeFileName; }
    public void setSelectedResumeFileName(String selectedResumeFileName) { this.selectedResumeFileName = selectedResumeFileName; }

    public String getTailoredCoverNote() { return tailoredCoverNote; }
    public void setTailoredCoverNote(String tailoredCoverNote) { this.tailoredCoverNote = tailoredCoverNote; }

    public String getWorkflowGuidance() { return workflowGuidance; }
    public void setWorkflowGuidance(String workflowGuidance) { this.workflowGuidance = workflowGuidance; }
}
