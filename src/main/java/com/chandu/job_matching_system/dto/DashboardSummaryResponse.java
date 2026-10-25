package com.chandu.job_matching_system.dto;

import java.util.List;
import java.util.Map;

public class DashboardSummaryResponse {

    private long totalApplications;
    private Map<String, Long> statusBreakdown;
    private long savedJobsCount;
    private long uploadedResumesCount;
    private double averageMatchScore;
    private List<JobApplicationResponse> upcomingFollowUps;
    private List<JobApplicationResponse> recentApplications;

    public DashboardSummaryResponse() {
    }

    public DashboardSummaryResponse(
            long totalApplications,
            Map<String, Long> statusBreakdown,
            long savedJobsCount,
            long uploadedResumesCount,
            double averageMatchScore,
            List<JobApplicationResponse> upcomingFollowUps,
            List<JobApplicationResponse> recentApplications) {
        this.totalApplications = totalApplications;
        this.statusBreakdown = statusBreakdown;
        this.savedJobsCount = savedJobsCount;
        this.uploadedResumesCount = uploadedResumesCount;
        this.averageMatchScore = averageMatchScore;
        this.upcomingFollowUps = upcomingFollowUps;
        this.recentApplications = recentApplications;
    }

    public long getTotalApplications() { return totalApplications; }
    public void setTotalApplications(long totalApplications) { this.totalApplications = totalApplications; }

    public Map<String, Long> getStatusBreakdown() { return statusBreakdown; }
    public void setStatusBreakdown(Map<String, Long> statusBreakdown) { this.statusBreakdown = statusBreakdown; }

    public long getSavedJobsCount() { return savedJobsCount; }
    public void setSavedJobsCount(long savedJobsCount) { this.savedJobsCount = savedJobsCount; }

    public long getUploadedResumesCount() { return uploadedResumesCount; }
    public void setUploadedResumesCount(long uploadedResumesCount) { this.uploadedResumesCount = uploadedResumesCount; }

    public double getAverageMatchScore() { return averageMatchScore; }
    public void setAverageMatchScore(double averageMatchScore) { this.averageMatchScore = averageMatchScore; }

    public List<JobApplicationResponse> getUpcomingFollowUps() { return upcomingFollowUps; }
    public void setUpcomingFollowUps(List<JobApplicationResponse> upcomingFollowUps) { this.upcomingFollowUps = upcomingFollowUps; }

    public List<JobApplicationResponse> getRecentApplications() { return recentApplications; }
    public void setRecentApplications(List<JobApplicationResponse> recentApplications) { this.recentApplications = recentApplications; }
}
