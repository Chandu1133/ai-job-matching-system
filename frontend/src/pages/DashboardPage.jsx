import React, { useEffect, useState } from "react";
import { api } from "../services/api.js";

export default function DashboardPage({ onNavigate }) {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadDashboard();
  }, []);

  async function loadDashboard() {
    setLoading(true);
    setError("");
    try {
      const data = await api.getDashboardSummary();
      setSummary(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  if (loading) return <div className="card">Loading dashboard analytics...</div>;
  if (error) return <div className="alert alert-error">{error}</div>;
  if (!summary) return null;

  return (
    <div>
      <div className="flex-between" style={{ marginBottom: "1rem" }}>
        <h2 style={{ margin: 0 }}>Candidate Dashboard</h2>
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <button className="btn btn-primary" onClick={() => onNavigate("jobs")}>
            Discover Jobs
          </button>
          <button className="btn btn-outline" onClick={() => onNavigate("resumes")}>
            Manage Resumes
          </button>
        </div>
      </div>

      <div className="grid-4" style={{ marginBottom: "1.25rem" }}>
        <div className="stat-card">
          <div style={{ color: "var(--muted)", fontSize: "0.85rem" }}>Total Applications</div>
          <div className="stat-value">{summary.totalApplications}</div>
        </div>
        <div className="stat-card">
          <div style={{ color: "var(--muted)", fontSize: "0.85rem" }}>Saved Bookmarks</div>
          <div className="stat-value">{summary.savedJobsCount}</div>
        </div>
        <div className="stat-card">
          <div style={{ color: "var(--muted)", fontSize: "0.85rem" }}>Uploaded Resumes</div>
          <div className="stat-value">{summary.uploadedResumesCount}</div>
        </div>
        <div className="stat-card">
          <div style={{ color: "var(--muted)", fontSize: "0.85rem" }}>Avg AI Match Score</div>
          <div className="stat-value">{summary.averageMatchScore}%</div>
        </div>
      </div>

      <div className="grid-2">
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Application Status Breakdown</h3>
          {summary.statusBreakdown &&
            Object.entries(summary.statusBreakdown).map(([status, count]) => (
              <div
                key={status}
                className="flex-between"
                style={{ padding: "0.45rem 0", borderBottom: "1px solid var(--border)" }}
              >
                <span className="badge badge-info">{status}</span>
                <strong>{count}</strong>
              </div>
            ))}
        </div>

        <div className="card">
          <h3 style={{ marginTop: 0 }}>Upcoming Follow-Ups</h3>
          {!summary.upcomingFollowUps || summary.upcomingFollowUps.length === 0 ? (
            <p style={{ color: "var(--muted)" }}>No upcoming follow-up dates scheduled.</p>
          ) : (
            summary.upcomingFollowUps.map((app) => (
              <div
                key={app.id}
                style={{ padding: "0.5rem 0", borderBottom: "1px solid var(--border)" }}
              >
                <strong>{app.jobTitle}</strong> at {app.company}
                <div style={{ fontSize: "0.85rem", color: "var(--muted)" }}>
                  Follow-up: {app.followUpDate} | Status: {app.status}
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      <div className="card">
        <div className="flex-between">
          <h3 style={{ marginTop: 0 }}>Recent Application Activity</h3>
          <button className="btn btn-outline" onClick={() => onNavigate("applications")}>
            View All Applications
          </button>
        </div>
        {!summary.recentApplications || summary.recentApplications.length === 0 ? (
          <p style={{ color: "var(--muted)" }}>
            You haven&apos;t tracked any job applications yet. Search jobs to get started!
          </p>
        ) : (
          summary.recentApplications.map((app) => (
            <div
              key={app.id}
              className="flex-between"
              style={{ padding: "0.6rem 0", borderBottom: "1px solid var(--border)" }}
            >
              <div>
                <strong>{app.jobTitle}</strong> — {app.company} ({app.location})
              </div>
              <span className="badge badge-success">{app.status}</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
