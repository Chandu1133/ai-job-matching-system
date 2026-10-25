import React, { useEffect, useState } from "react";
import { api } from "../services/api.js";

export default function MatchingAndOptimizationPage({ initialJob }) {
  const [jobs, setJobs] = useState([]);
  const [resumes, setResumes] = useState([]);
  const [savedOptimizations, setSavedOptimizations] = useState([]);
  const [selectedJobId, setSelectedJobId] = useState(initialJob?.id || "");
  const [selectedResumeId, setSelectedResumeId] = useState("");
  const [matchResult, setMatchResult] = useState(null);
  const [optimization, setOptimization] = useState(null);
  const [editablePreview, setEditablePreview] = useState("");
  const [loading, setLoading] = useState(false);
  const [uploadingResume, setUploadingResume] = useState(false);
  const [deletingOptId, setDeletingOptId] = useState(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    loadInitialData();
  }, []);

  async function loadInitialData() {
    try {
      const [jobsPage, resumeList, optList] = await Promise.all([
        api.searchJobs({ page: 0, size: 30 }),
        api.getResumes(),
        api.getMyOptimizations().catch(() => [])
      ]);
      const jobItems = jobsPage.content || [];
      setJobs(jobItems);
      setResumes(resumeList || []);
      setSavedOptimizations(optList || []);

      if (!selectedJobId && jobItems.length > 0) {
        setSelectedJobId(jobItems[0].id);
      }
      if (resumeList && resumeList.length > 0) {
        setSelectedResumeId(resumeList[0].id);
      }
    } catch (err) {
      setError(err.message);
    }
  }

  async function refreshOptimizationsList() {
    try {
      const optList = await api.getMyOptimizations();
      setSavedOptimizations(optList || []);
    } catch {
      // Ignore refresh error
    }
  }

  async function handleQuickResumeUpload(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setError("");
    setSuccess("");
    setUploadingResume(true);
    try {
      const uploaded = await api.uploadResume(file);
      const updatedResumes = await api.getResumes();
      setResumes(updatedResumes || []);
      setSelectedResumeId(uploaded.id);
      setSuccess(`Resume "${uploaded.originalFileName}" uploaded and selected!`);
    } catch (err) {
      setError(err.message);
    } finally {
      setUploadingResume(false);
      e.target.value = "";
    }
  }

  async function handleRunMatch() {
    if (!selectedJobId) return;
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      const res = await api.calculateMatch(selectedJobId, selectedResumeId || undefined);
      setMatchResult(res);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleGenerateOptimization() {
    if (!selectedJobId || !selectedResumeId) {
      setError("Please upload/select a resume and select a target job to optimize.");
      return;
    }
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      const opt = await api.generateOptimization(selectedResumeId, selectedJobId);
      setOptimization(opt);
      setEditablePreview(opt.revisedResumePreview || "");
      await refreshOptimizationsList();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleApproveOptimization(applyToOriginalResume) {
    if (!optimization) return;
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      const approved = await api.approveOptimization(optimization.id, {
        revisedResumePreview: editablePreview,
        applyToOriginalResume
      });
      setOptimization(approved);
      setEditablePreview(approved.revisedResumePreview || editablePreview);
      setSuccess(
        applyToOriginalResume
          ? "Resume optimization approved, saved, and applied to your resume!"
          : "Resume optimization approved and saved!"
      );
      await refreshOptimizationsList();
      if (applyToOriginalResume) {
        const updatedResumes = await api.getResumes().catch(() => null);
        if (updatedResumes) setResumes(updatedResumes);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleDeleteOptimization(id) {
    setError("");
    setSuccess("");
    setDeletingOptId(id);
    try {
      await api.deleteOptimization(id);
      if (optimization && optimization.id === id) {
        setOptimization(null);
        setEditablePreview("");
      }
      setSuccess("Optimized resume deleted successfully.");
      await refreshOptimizationsList();
    } catch (err) {
      setError(err.message);
    } finally {
      setDeletingOptId(null);
    }
  }

  function handleDownloadOptimizedResume(text, jobTitle) {
    const safeTitle = (jobTitle || "Optimized_Resume").replace(/[^a-zA-Z0-9_-]/g, "_");
    const blob = new Blob([text || ""], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `Tailored_Resume_${safeTitle}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.appendChild !== undefined && document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  async function handleCopyOptimizedResume(text) {
    try {
      await navigator.clipboard.writeText(text || "");
      setSuccess("Optimized resume copied to clipboard!");
    } catch {
      setError("Unable to copy automatically. Please select and copy from the preview box.");
    }
  }

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>AI/NLP Job Matching & Truthful Resume Optimization</h2>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="card">
        <div className="grid-2">
          <div className="form-group">
            <label>Select Target Job</label>
            <select
              className="form-control"
              value={selectedJobId}
              onChange={(e) => setSelectedJobId(e.target.value)}
            >
              {jobs.map((j) => (
                <option key={j.id} value={j.id}>
                  {j.title} — {j.company} ({j.location})
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label>Select Candidate Resume (or Upload Below)</label>
            <select
              className="form-control"
              value={selectedResumeId}
              onChange={(e) => setSelectedResumeId(e.target.value)}
            >
              <option value="">Use Candidate Profile Only</option>
              {resumes.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.originalFileName} ({r.extractionStatus})
                </option>
              ))}
            </select>
            <div style={{ marginTop: "0.55rem", fontSize: "0.86rem" }}>
              <label style={{ display: "block", marginBottom: "0.25rem", color: "var(--muted)" }}>
                Upload a Previous / New Resume (PDF or DOCX):
              </label>
              <input
                type="file"
                accept=".pdf,.docx"
                onChange={handleQuickResumeUpload}
                disabled={uploadingResume}
              />
              {uploadingResume && <span style={{ marginLeft: "0.5rem" }}>Uploading & parsing...</span>}
            </div>
          </div>
        </div>

        <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap" }}>
          <button className="btn btn-primary" onClick={handleRunMatch} disabled={loading}>
            Calculate Explainable Match Score
          </button>
          <button className="btn btn-outline" onClick={handleGenerateOptimization} disabled={loading}>
            Generate Truthful Resume Optimization Preview
          </button>
        </div>
      </div>

      {matchResult && (
        <div className="card">
          <div className="flex-between">
            <h3 style={{ margin: 0 }}>
              Match Result: {matchResult.jobTitle} ({matchResult.company})
            </h3>
            <span className="stat-value">{matchResult.matchScore} / 100</span>
          </div>
          <p style={{ marginTop: "0.75rem" }}>{matchResult.explanation}</p>

          <div className="grid-2">
            <div>
              <strong>Matched Skills ({matchResult.matchedSkills?.length || 0}):</strong>
              <div className="skill-tags">
                {(matchResult.matchedSkills || []).map((s) => (
                  <span key={s} className="badge badge-success">
                    ✓ {s}
                  </span>
                ))}
              </div>
            </div>
            <div>
              <strong>Missing / Unidentified Skills ({matchResult.missingSkills?.length || 0}):</strong>
              <div className="skill-tags">
                {(matchResult.missingSkills || []).map((s) => (
                  <span key={s} className="badge badge-danger">
                    ✕ {s}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {optimization && (
        <div className="card">
          <div className="flex-between">
            <h3 style={{ marginTop: 0 }}>
              Truthful Optimization Preview — {optimization.jobTitle} ({optimization.status})
            </h3>
            <span className={optimization.approvedByCandidate ? "badge badge-success" : "badge badge-info"}>
              {optimization.approvedByCandidate ? "✓ Approved by Candidate" : "Pending Candidate Review"}
            </span>
          </div>

          <p style={{ fontSize: "0.9rem", color: "var(--muted)" }}>
            Only verified qualifications from your profile and resume are included below. No unverified skills are fabricated.
          </p>

          <h4>Actionable Recommendations:</h4>
          <ul>
            {(optimization.suggestions || []).map((sug, i) => (
              <li key={i}>{sug}</li>
            ))}
          </ul>

          <div className="form-group">
            <label>Tailored Optimized Resume Content (Editable):</label>
            <textarea
              className="form-control"
              style={{ minHeight: 260, fontFamily: "monospace" }}
              value={editablePreview}
              onChange={(e) => setEditablePreview(e.target.value)}
            />
          </div>

          <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap" }}>
            <button
              className="btn btn-primary"
              onClick={() => handleApproveOptimization(false)}
              disabled={loading}
            >
              Approve Optimization Record
            </button>
            <button
              className="btn btn-outline"
              onClick={() => handleApproveOptimization(true)}
              disabled={loading}
            >
              Approve & Apply to Resume
            </button>
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => handleDownloadOptimizedResume(editablePreview, optimization.jobTitle)}
            >
              Download Optimized Resume (.txt)
            </button>
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => handleCopyOptimizedResume(editablePreview)}
            >
              Copy Optimized Resume
            </button>
            <button
              type="button"
              className="btn btn-danger"
              onClick={() => handleDeleteOptimization(optimization.id)}
              disabled={deletingOptId === optimization.id}
            >
              {deletingOptId === optimization.id ? "Deleting..." : "Delete Optimized Resume"}
            </button>
          </div>
        </div>
      )}

      <div className="card">
        <h3 style={{ marginTop: 0 }}>My Saved & Approved Optimized Resumes</h3>
        {savedOptimizations.length === 0 ? (
          <p style={{ color: "var(--muted)" }}>
            No resume optimizations generated yet. Click &ldquo;Generate Truthful Resume Optimization Preview&rdquo; above to create one.
          </p>
        ) : (
          savedOptimizations.map((opt) => (
            <div
              key={opt.id}
              className="flex-between"
              style={{ padding: "0.75rem 0", borderBottom: "1px solid var(--border)" }}
            >
              <div>
                <strong>{opt.jobTitle}</strong> — {opt.company}{" "}
                <span className={opt.approvedByCandidate ? "badge badge-success" : "badge badge-info"}>
                  {opt.status}
                </span>
                <div style={{ fontSize: "0.85rem", color: "var(--muted)", marginTop: "0.2rem" }}>
                  Base Resume: {opt.originalFileName} | Aligned Skills: {(opt.highlightedSkills || []).join(", ") || "N/A"}
                </div>
              </div>
              <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={() => {
                    setOptimization(opt);
                    setEditablePreview(opt.revisedResumePreview || "");
                  }}
                >
                  View Optimized Resume
                </button>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => handleDownloadOptimizedResume(opt.revisedResumePreview, opt.jobTitle)}
                >
                  Download (.txt)
                </button>
                <button
                  type="button"
                  className="btn btn-danger"
                  onClick={() => handleDeleteOptimization(opt.id)}
                  disabled={deletingOptId === opt.id}
                >
                  {deletingOptId === opt.id ? "Deleting..." : "Delete"}
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
