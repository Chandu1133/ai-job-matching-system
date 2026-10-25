import React, { useEffect, useState } from "react";
import { api } from "../services/api.js";

export default function ResumesPage() {
  const [resumes, setResumes] = useState([]);
  const [selectedResume, setSelectedResume] = useState(null);
  const [reviewForm, setReviewForm] = useState({
    extractedSkills: "",
    extractedEducation: "",
    extractedExperience: "",
    extractedProjects: "",
    extractedText: ""
  });
  const [uploading, setUploading] = useState(false);
  const [savingReview, setSavingReview] = useState(false);
  const [deletingId, setDeletingId] = useState(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    loadResumes(true);
  }, []);

  async function loadResumes(autoSelectFirst = false) {
    try {
      const list = await api.getResumes();
      const safeList = list || [];
      setResumes(safeList);
      if (safeList.length === 0) {
        setSelectedResume(null);
        setReviewForm({
          extractedSkills: "",
          extractedEducation: "",
          extractedExperience: "",
          extractedProjects: "",
          extractedText: ""
        });
      } else if (autoSelectFirst) {
        selectResumeForReview(safeList[0]);
      }
    } catch (err) {
      setError(err.message);
    }
  }

  function selectResumeForReview(r) {
    setSelectedResume(r);
    setReviewForm({
      extractedSkills: r.extractedSkills || "",
      extractedEducation: r.extractedEducation || "",
      extractedExperience: r.extractedExperience || "",
      extractedProjects: r.extractedProjects || "",
      extractedText: r.extractedText || ""
    });
  }

  async function handleFileUpload(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setError("");
    setSuccess("");
    setUploading(true);
    try {
      const uploaded = await api.uploadResume(file);
      setSuccess(uploaded.message || "Resume uploaded and parsed!");
      await loadResumes(false);
      selectResumeForReview(uploaded);
    } catch (err) {
      setError(err.message);
    } finally {
      setUploading(false);
      e.target.value = "";
    }
  }

  async function handleSaveReview(e) {
    e.preventDefault();
    if (!selectedResume) return;
    setSavingReview(true);
    setError("");
    setSuccess("");
    try {
      const updated = await api.reviewResume(selectedResume.id, reviewForm);
      setSuccess(updated.message || "Reviewed resume saved!");
      selectResumeForReview(updated);
      await loadResumes(false);
    } catch (err) {
      setError(err.message);
    } finally {
      setSavingReview(false);
    }
  }

  async function handleDelete(id) {
    setError("");
    setSuccess("");
    setDeletingId(id);
    try {
      await api.deleteResume(id);
      setSuccess("Resume deleted successfully.");
      await loadResumes(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>Resume Management & Candidate Review</h2>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Upload PDF or DOCX Resume (Max 5 MB)</h3>
        <input
          type="file"
          accept=".pdf,.docx"
          onChange={handleFileUpload}
          disabled={uploading}
        />
        {uploading && <span style={{ marginLeft: "1rem" }}>Uploading & extracting text...</span>}
      </div>

      <div className="grid-2">
        <div className="card">
          <h3 style={{ marginTop: 0 }}>My Uploaded Resumes</h3>
          {resumes.length === 0 ? (
            <p style={{ color: "var(--muted)" }}>No resumes uploaded yet.</p>
          ) : (
            resumes.map((r) => (
              <div
                key={r.id}
                className="flex-between"
                style={{ padding: "0.65rem 0", borderBottom: "1px solid var(--border)" }}
              >
                <div>
                  <strong>{r.originalFileName}</strong> ({r.fileType})
                  <div style={{ fontSize: "0.82rem", color: "var(--muted)" }}>
                    Status: {r.extractionStatus} |{" "}
                    {r.reviewedByCandidate ? "✓ Candidate Reviewed" : "Pending Review"}
                  </div>
                </div>
                <div style={{ display: "flex", gap: "0.4rem" }}>
                  <button
                    type="button"
                    className="btn btn-outline"
                    onClick={() => selectResumeForReview(r)}
                  >
                    Review / Edit
                  </button>
                  <button
                    type="button"
                    className="btn btn-danger"
                    onClick={() => handleDelete(r.id)}
                    disabled={deletingId === r.id}
                  >
                    {deletingId === r.id ? "Deleting..." : "Delete"}
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        <div className="card">
          <h3 style={{ marginTop: 0 }}>Review & Correct Extracted Information</h3>
          {!selectedResume ? (
            <p style={{ color: "var(--muted)" }}>Select an uploaded resume to inspect and verify extracted sections.</p>
          ) : (
            <form onSubmit={handleSaveReview}>
              <div className="form-group">
                <label>Extracted Skills</label>
                <input
                  className="form-control"
                  value={reviewForm.extractedSkills}
                  onChange={(e) => setReviewForm({ ...reviewForm, extractedSkills: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Extracted Education</label>
                <textarea
                  className="form-control"
                  rows={4}
                  value={reviewForm.extractedEducation}
                  onChange={(e) => setReviewForm({ ...reviewForm, extractedEducation: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Extracted Work Experience / Certifications</label>
                <textarea
                  className="form-control"
                  rows={4}
                  value={reviewForm.extractedExperience}
                  onChange={(e) => setReviewForm({ ...reviewForm, extractedExperience: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Extracted Projects</label>
                <textarea
                  className="form-control"
                  rows={5}
                  value={reviewForm.extractedProjects}
                  onChange={(e) => setReviewForm({ ...reviewForm, extractedProjects: e.target.value })}
                />
              </div>
              <button type="submit" className="btn btn-primary" disabled={savingReview}>
                {savingReview ? "Saving..." : "Confirm & Save Reviewed Details"}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
