package com.chandu.job_matching_system.dto;

import java.time.Instant;
import java.util.List;

public class JobMatchResponse {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private String company;
    private Long resumeId;
    private int matchScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String explanation;
    private Instant calculatedAt;

    public JobMatchResponse() {
    }

    public JobMatchResponse(
            Long id,
            Long jobId,
            String jobTitle,
            String company,
            Long resumeId,
            int matchScore,
            List<String> matchedSkills,
            List<String> missingSkills,
            String explanation,
            Instant calculatedAt) {
        this.id = id;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.company = company;
        this.resumeId = resumeId;
        this.matchScore = matchScore;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.explanation = explanation;
        this.calculatedAt = calculatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }

    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }

    public List<String> getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(List<String> matchedSkills) { this.matchedSkills = matchedSkills; }

    public List<String> getMissingSkills() { return missingSkills; }
    public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
}
