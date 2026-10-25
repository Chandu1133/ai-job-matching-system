package com.chandu.job_matching_system.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "resume_optimizations")
public class ResumeOptimization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(length = 1000)
    private String supportedKeywords;

    @Column(length = 1000)
    private String missingSkills;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String suggestions;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String revisedResumePreview;

    @Column(nullable = false, length = 40)
    private String status = "PREVIEW_PENDING_APPROVAL";

    @Column(nullable = false)
    private boolean approvedByCandidate = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant approvedAt;

    public ResumeOptimization() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public String getSupportedKeywords() { return supportedKeywords; }
    public void setSupportedKeywords(String supportedKeywords) { this.supportedKeywords = supportedKeywords; }

    public String getMissingSkills() { return missingSkills; }
    public void setMissingSkills(String missingSkills) { this.missingSkills = missingSkills; }

    public String getSuggestions() { return suggestions; }
    public void setSuggestions(String suggestions) { this.suggestions = suggestions; }

    public String getRevisedResumePreview() { return revisedResumePreview; }
    public void setRevisedResumePreview(String revisedResumePreview) { this.revisedResumePreview = revisedResumePreview; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isApprovedByCandidate() { return approvedByCandidate; }
    public void setApprovedByCandidate(boolean approvedByCandidate) { this.approvedByCandidate = approvedByCandidate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
}
