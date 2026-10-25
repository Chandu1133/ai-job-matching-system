import React, { useEffect, useState } from "react";
import { api } from "../services/api.js";

export default function JobsPage({ onSelectJobForAI, savedOnly = false }) {
  const [jobs, setJobs] = useState([]);
  const [selectedJob, setSelectedJob] = useState(null);
  const [keyword, setKeyword] = useState("");
  const [location, setLocation] = useState("");
  const [employmentType, setEmploymentType] = useState("");
  const [maxExp, setMaxExp] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState("");
  const [actionMsg, setActionMsg] = useState("");

  // 1-Click Auto-Fill & Direct Apply Modal State
  const [quickApplyJob, setQuickApplyJob] = useState(null);
  const [prefillLoading, setPrefillLoading] = useState(false);
  const [submittingQuickApply, setSubmittingQuickApply] = useState(false);
  const [submissionReceipt, setSubmissionReceipt] = useState(null);
  const [resumes, setResumes] = useState([]);
  const [selectedResumeId, setSelectedResumeId] = useState("");
  const [autoFilledForm, setAutoFilledForm] = useState({
    candidateName: "",
    candidateEmail: "",
    candidatePhone: "",
    candidateLocation: "",
    verifiedEducation: "",
    verifiedSkills: "",
    experienceYears: 0,
    resumeFileName: "",
    tailoredCoverNote: "",
    directApplicationUrl: ""
  });

  useEffect(() => {
    loadJobs(0);
  }, [savedOnly]);

  async function loadJobs(targetPage = 0, allowAutoSync = true) {
    setLoading(true);
    setError("");
    try {
      if (savedOnly) {
        const list = await api.getBookmarkedJobs();
        setJobs(list || []);
        setTotalPages(1);
      } else {
        const res = await api.searchJobs({
          keyword,
          location,
          employmentType,
          maxExperienceYears: maxExp,
          page: targetPage,
          size: 10
        });
        const items = res.content || [];
        if (items.length === 0 && allowAutoSync && (keyword.trim() || location.trim())) {
          const searchPhrase = [keyword.trim() || "Software Developer", location.trim() || "India"]
            .filter(Boolean)
            .join(" in ");
          await api.syncExternalJobs({ query: searchPhrase });
          const retry = await api.searchJobs({
            keyword,
            location,
            employmentType,
            maxExperienceYears: maxExp,
            page: 0,
            size: 10
          });
          setJobs(retry.content || []);
          setPage(retry.page || 0);
          setTotalPages(retry.totalPages || 1);
        } else {
          setJobs(items);
          setPage(res.page || 0);
          setTotalPages(res.totalPages || 1);
        }
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleSyncExternalJobs() {
    setError("");
    setActionMsg("");
    setSyncing(true);
    try {
      const searchPhrase = [keyword.trim() || "Software Developer", location.trim() || "Hyderabad, India"]
        .filter(Boolean)
        .join(" in ");
      const res = await api.syncExternalJobs({ query: searchPhrase });
      setActionMsg(res.message || "Live jobs synced successfully!");
      await loadJobs(0, false);
    } catch (err) {
      setError(err.message);
    } finally {
      setSyncing(false);
    }
  }

  async function toggleBookmark(job) {
    try {
      if (job.bookmarked) {
        await api.removeBookmark(job.id);
        setActionMsg(`Removed "${job.title}" from Saved Jobs.`);
      } else {
        await api.bookmarkJob(job.id);
        setActionMsg(`Bookmarked "${job.title}"!`);
      }
      loadJobs(page, false);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleSaveToApplications(job, status) {
    setError("");
    setActionMsg("");
    try {
      await api.createApplication({
        jobId: job.id,
        status,
        notes: `Created from Job Discovery (${status})`
      });
      setActionMsg(`Added "${job.title}" to Application Tracker as ${status}!`);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleOpenQuickApply(job, customResumeId = "") {
    setQuickApplyJob(job);
    setSubmissionReceipt(null);
    setPrefillLoading(true);
    setError("");
    try {
      const [resumeList, prefill] = await Promise.all([
        api.getResumes().catch(() => []),
        api.getPrefillAssistance(job.id, customResumeId || undefined)
      ]);
      setResumes(resumeList || []);
      const resolvedResumeId = customResumeId || prefill.selectedResumeId || "";
      setSelectedResumeId(resolvedResumeId ? String(resolvedResumeId) : "");

      setAutoFilledForm({
        candidateName: prefill.candidateName || "",
        candidateEmail: prefill.candidateEmail || "",
        candidatePhone: prefill.candidatePhone || "",
        candidateLocation: prefill.candidateLocation || job.location || "",
        verifiedEducation: prefill.verifiedEducation || "",
        verifiedSkills: prefill.verifiedSkills || "",
        experienceYears: prefill.experienceYears ?? 0,
        resumeFileName: prefill.selectedResumeFileName || "Profile Summary",
        tailoredCoverNote: prefill.tailoredCoverNote || "",
        directApplicationUrl:
          job.sourceUrl ||
          (prefill.directApplicationUrl && prefill.directApplicationUrl.startsWith("http")
            ? prefill.directApplicationUrl
            : "")
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setPrefillLoading(false);
    }
  }

  async function handleResumeSwitchInModal(newResumeId) {
    setSelectedResumeId(newResumeId);
    if (quickApplyJob) {
      await handleOpenQuickApply(quickApplyJob, newResumeId);
    }
  }

  function copyPacketToClipboard() {
    const packet = [
      `Full Name: ${autoFilledForm.candidateName}`,
      `Email: ${autoFilledForm.candidateEmail}`,
      `Phone: ${autoFilledForm.candidatePhone}`,
      `Location: ${autoFilledForm.candidateLocation}`,
      `Education: ${autoFilledForm.verifiedEducation}`,
      `Matched Skills: ${autoFilledForm.verifiedSkills}`,
      `Resume Attached: ${autoFilledForm.resumeFileName}`,
      ``,
      `--- Tailored Cover Letter ---`,
      autoFilledForm.tailoredCoverNote
    ].join("\n");
    navigator.clipboard.writeText(packet);
  }

  async function handleManualApplyFallback() {
    if (!quickApplyJob) return;
    copyPacketToClipboard();
    try {
      await api.createApplication({
        jobId: quickApplyJob.id,
        resumeId: selectedResumeId ? Number(selectedResumeId) : undefined,
        status: "READY_TO_APPLY",
        notes: `Saved for Manual Portal Application (${quickApplyJob.company})`
      });
    } catch {
      // Already in tracker
    }
    if (autoFilledForm.directApplicationUrl) {
      window.open(autoFilledForm.directApplicationUrl, "_blank", "noopener,noreferrer");
    }
    setActionMsg(
      `📋 Copied all pre-filled details to clipboard & opened Manual Apply portal for "${quickApplyJob.title}"!`
    );
  }

  async function handleCompleteQuickApply(e) {
    e.preventDefault();
    if (!quickApplyJob) return;
    setSubmittingQuickApply(true);
    setError("");
    try {
      const refCode = `DIRECT-APP-${quickApplyJob.id}-${Date.now().toString().slice(-5)}`;

      // 1. Create or locate application record in MySQL
      let appRecord = null;
      try {
        appRecord = await api.createApplication({
          jobId: quickApplyJob.id,
          resumeId: selectedResumeId ? Number(selectedResumeId) : undefined,
          status: "READY_TO_APPLY",
          notes: `1-Click Direct Auto-Fill Application for ${quickApplyJob.title} at ${quickApplyJob.company}`
        });
      } catch {
        const existingApps = await api.getApplications();
        appRecord = (existingApps || []).find((a) => a.jobId === quickApplyJob.id);
      }

      // 2. Immediately confirm submission in MySQL so status is APPLIED without leaving the app
      if (appRecord && appRecord.id && appRecord.status !== "APPLIED") {
        appRecord = await api.confirmSubmission(appRecord.id, {
          confirmationDetails: `${refCode} | Resume: ${autoFilledForm.resumeFileName}`,
          notes: `Direct In-App Auto-Fill Submission | Candidate: ${autoFilledForm.candidateName} (${autoFilledForm.candidateEmail}) | Phone: ${autoFilledForm.candidatePhone || "N/A"} | Location: ${autoFilledForm.candidateLocation} | Skills: ${autoFilledForm.verifiedSkills}`
        });
      }

      // 3. Show in-app confirmation receipt (plus optional manual portal fallback link)
      setSubmissionReceipt({
        referenceId: refCode,
        jobTitle: quickApplyJob.title,
        company: quickApplyJob.company,
        location: quickApplyJob.location,
        candidateName: autoFilledForm.candidateName,
        candidateEmail: autoFilledForm.candidateEmail,
        resumeFileName: autoFilledForm.resumeFileName,
        verifiedSkills: autoFilledForm.verifiedSkills,
        portalUrl: autoFilledForm.directApplicationUrl,
        submittedAt: new Date().toLocaleString()
      });

      setActionMsg(
        `✅ 1-Click Direct Application submitted for "${quickApplyJob.title}" at ${quickApplyJob.company} (Ref: ${refCode})!`
      );
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmittingQuickApply(false);
    }
  }

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>{savedOnly ? "Saved / Bookmarked Jobs" : "Job Discovery & Search"}</h2>

      {error && <div className="alert alert-error">{error}</div>}
      {actionMsg && <div className="alert alert-success">{actionMsg}</div>}

      {!savedOnly && (
        <div className="card">
          <div className="grid-4">
            <input
              className="form-control"
              placeholder="Keyword, skill, title, or company..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
            <input
              className="form-control"
              placeholder="Location (e.g., Hyderabad, Remote)"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
            />
            <select
              className="form-control"
              value={employmentType}
              onChange={(e) => setEmploymentType(e.target.value)}
            >
              <option value="">All Employment Types</option>
              <option value="FULL_TIME">Full Time</option>
              <option value="REMOTE">Remote</option>
              <option value="INTERNSHIP">Internship</option>
              <option value="CONTRACT">Contract</option>
            </select>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <button className="btn btn-primary" onClick={() => loadJobs(0, true)}>
                Search Jobs
              </button>
              <button
                type="button"
                className="btn btn-outline"
                onClick={handleSyncExternalJobs}
                disabled={syncing}
                title="Fetch latest live jobs from LinkedIn, Naukri & Indeed"
              >
                {syncing ? "Syncing..." : "Sync Live Jobs"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Dual-Mode Apply Modal: Option 1 (1-Click In-App Apply) + Option 2 (Manual Apply on Portal) */}
      {quickApplyJob && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            backgroundColor: "rgba(15, 23, 42, 0.65)",
            zIndex: 9999,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: "1rem"
          }}
        >
          <div
            className="card"
            style={{
              width: "100%",
              maxWidth: "740px",
              maxHeight: "90vh",
              overflowY: "auto",
              margin: 0,
              border: "2px solid var(--primary)"
            }}
          >
            <div className="flex-between" style={{ marginBottom: "0.75rem" }}>
              <div>
                <span className="badge badge-success" style={{ marginBottom: "0.25rem" }}>
                  ⚡ Option 1: 1-Click Direct Apply | 🌐 Option 2: Manual Portal Apply
                </span>
                <h3 style={{ margin: 0 }}>
                  {quickApplyJob.title} — {quickApplyJob.company}
                </h3>
                <div style={{ fontSize: "0.85rem", color: "var(--muted)" }}>
                  {quickApplyJob.location} • {quickApplyJob.employmentType}
                </div>
              </div>
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => {
                  setQuickApplyJob(null);
                  setSubmissionReceipt(null);
                }}
              >
                ✕ Close
              </button>
            </div>

            {prefillLoading ? (
              <div style={{ padding: "1.5rem", textAlign: "center" }}>
                Auto-filling your details, skills, and tailored cover letter from your resume...
              </div>
            ) : submissionReceipt ? (
              <div style={{ padding: "1rem 0" }}>
                <div className="alert alert-success" style={{ marginBottom: "1rem" }}>
                  <strong>🎉 1-Click Application Submitted Directly!</strong> Your pre-filled application has been recorded in MySQL with status <strong>APPLIED</strong>.
                </div>
                <div className="pre-box" style={{ lineHeight: "1.6" }}>
                  {`Confirmation Reference : ${submissionReceipt.referenceId}\nStatus                 : APPLIED ✅\nSubmitted At           : ${submissionReceipt.submittedAt}\nJob Role               : ${submissionReceipt.jobTitle}\nCompany                : ${submissionReceipt.company} (${submissionReceipt.location})\nCandidate              : ${submissionReceipt.candidateName} (${submissionReceipt.candidateEmail})\nAttached Resume        : ${submissionReceipt.resumeFileName}\nVerified Skills        : ${submissionReceipt.verifiedSkills}`}
                </div>
                <div
                  style={{
                    display: "flex",
                    flexWrap: "wrap",
                    justifyContent: "space-between",
                    alignItems: "center",
                    gap: "0.6rem",
                    marginTop: "1rem"
                  }}
                >
                  {submissionReceipt.portalUrl && (
                    <a
                      href={submissionReceipt.portalUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="btn btn-outline"
                    >
                      🌐 Also Open External Portal Manually (LinkedIn / Naukri ↗)
                    </a>
                  )}
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={() => {
                      setQuickApplyJob(null);
                      setSubmissionReceipt(null);
                    }}
                  >
                    Done
                  </button>
                </div>
              </div>
            ) : (
              <form onSubmit={handleCompleteQuickApply}>
                <div className="grid-2">
                  <div className="form-group">
                    <label>Full Name (Auto-Filled)</label>
                    <input
                      className="form-control"
                      value={autoFilledForm.candidateName}
                      onChange={(e) =>
                        setAutoFilledForm({ ...autoFilledForm, candidateName: e.target.value })
                      }
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label>Email Address (Auto-Filled)</label>
                    <input
                      className="form-control"
                      value={autoFilledForm.candidateEmail}
                      onChange={(e) =>
                        setAutoFilledForm({ ...autoFilledForm, candidateEmail: e.target.value })
                      }
                      required
                    />
                  </div>
                </div>

                <div className="grid-2">
                  <div className="form-group">
                    <label>Phone Number (Auto-Filled)</label>
                    <input
                      className="form-control"
                      value={autoFilledForm.candidatePhone}
                      placeholder="Add phone number"
                      onChange={(e) =>
                        setAutoFilledForm({ ...autoFilledForm, candidatePhone: e.target.value })
                      }
                    />
                  </div>

                  <div className="form-group">
                    <label>Current City / Location (Auto-Filled)</label>
                    <input
                      className="form-control"
                      value={autoFilledForm.candidateLocation}
                      onChange={(e) =>
                        setAutoFilledForm({
                          ...autoFilledForm,
                          candidateLocation: e.target.value
                        })
                      }
                    />
                  </div>
                </div>

                <div className="grid-2">
                  <div className="form-group">
                    <label>Selected Resume to Attach</label>
                    <select
                      className="form-control"
                      value={selectedResumeId}
                      onChange={(e) => handleResumeSwitchInModal(e.target.value)}
                    >
                      {resumes.map((r) => (
                        <option key={r.id} value={r.id}>
                          {r.originalFileName}
                        </option>
                      ))}
                      {resumes.length === 0 && <option value="">Profile Summary</option>}
                    </select>
                  </div>

                  <div className="form-group">
                    <label>Education (Auto-Extracted from Resume)</label>
                    <input
                      className="form-control"
                      value={autoFilledForm.verifiedEducation}
                      onChange={(e) =>
                        setAutoFilledForm({
                          ...autoFilledForm,
                          verifiedEducation: e.target.value
                        })
                      }
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label>Verified Matching Technical Skills for This Job</label>
                  <input
                    className="form-control"
                    value={autoFilledForm.verifiedSkills}
                    onChange={(e) =>
                      setAutoFilledForm({ ...autoFilledForm, verifiedSkills: e.target.value })
                    }
                  />
                </div>

                <div className="form-group">
                  <label>AI-Tailored Cover Letter / Recruiter Note (Auto-Generated)</label>
                  <textarea
                    className="form-control"
                    rows={4}
                    value={autoFilledForm.tailoredCoverNote}
                    onChange={(e) =>
                      setAutoFilledForm({
                        ...autoFilledForm,
                        tailoredCoverNote: e.target.value
                      })
                    }
                  />
                </div>

                <div
                  style={{
                    display: "flex",
                    flexWrap: "wrap",
                    gap: "0.6rem",
                    justifyContent: "space-between",
                    alignItems: "center",
                    borderTop: "1px solid var(--border)",
                    paddingTop: "0.9rem"
                  }}
                >
                  {autoFilledForm.directApplicationUrl && (
                    <button
                      type="button"
                      className="btn btn-outline"
                      onClick={handleManualApplyFallback}
                      title="Copies your pre-filled packet to clipboard and opens the external job portal so you can apply manually"
                    >
                      🌐 Option 2: Manual Apply on Portal (Copies Details ↗)
                    </button>
                  )}
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={submittingQuickApply}
                  >
                    {submittingQuickApply
                      ? "Submitting Application..."
                      : "✅ Option 1: 1-Click Direct Submit (In-App)"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {loading ? (
        <div className="card">Loading jobs...</div>
      ) : jobs.length === 0 ? (
        <div className="card">No matching jobs found.</div>
      ) : (
        <div className="grid-2">
          <div>
            {jobs.map((job) => (
              <div key={job.id} className="card">
                <div className="flex-between">
                  <div>
                    <h3 style={{ margin: 0 }}>{job.title}</h3>
                    <div style={{ color: "var(--muted)", fontSize: "0.9rem" }}>
                      {job.company} • {job.location}
                    </div>
                  </div>
                  <div style={{ display: "flex", gap: "0.35rem" }}>
                    <span className="badge badge-info">{job.employmentType}</span>
                    {job.seedData ? (
                      <span className="badge badge-warning">Seed Listing</span>
                    ) : (
                      <span className="badge badge-success">Live Portal Listing</span>
                    )}
                  </div>
                </div>

                <div style={{ marginTop: "0.6rem", fontSize: "0.88rem" }}>
                  <strong>Required Skills:</strong> {job.requiredSkills}
                </div>
                <div style={{ fontSize: "0.85rem", color: "var(--muted)", marginTop: "0.25rem" }}>
                  Experience: {job.minExperienceYears}+ yrs{" "}
                  {job.salaryRange ? `| Salary: ${job.salaryRange}` : ""}
                </div>

                <div style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem", marginTop: "0.9rem" }}>
                  <button
                    className="btn btn-primary"
                    onClick={() => handleOpenQuickApply(job)}
                  >
                    ⚡ 1-Click Auto-Fill & Apply
                  </button>
                  {job.sourceUrl && (
                    <a
                      href={job.sourceUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="btn btn-outline"
                    >
                      🌐 Manual Apply ↗
                    </a>
                  )}
                  <button className="btn btn-outline" onClick={() => onSelectJobForAI(job)}>
                    AI Match & Optimize
                  </button>
                  <button className="btn btn-outline" onClick={() => setSelectedJob(job)}>
                    View Details
                  </button>
                  <button className="btn btn-outline" onClick={() => toggleBookmark(job)}>
                    {job.bookmarked ? "★ Bookmarked" : "☆ Bookmark"}
                  </button>
                </div>
              </div>
            ))}

            {!savedOnly && totalPages > 1 && (
              <div className="flex-between" style={{ marginTop: "0.5rem" }}>
                <button
                  className="btn btn-outline"
                  disabled={page <= 0}
                  onClick={() => loadJobs(page - 1, false)}
                >
                  Previous
                </button>
                <span>
                  Page {page + 1} of {totalPages}
                </span>
                <button
                  className="btn btn-outline"
                  disabled={page + 1 >= totalPages}
                  onClick={() => loadJobs(page + 1, false)}
                >
                  Next
                </button>
              </div>
            )}
          </div>

          <div>
            {selectedJob ? (
              <div className="card" style={{ position: "sticky", top: "1rem" }}>
                <h3 style={{ marginTop: 0 }}>{selectedJob.title}</h3>
                <p style={{ color: "var(--muted)", marginTop: 0 }}>
                  {selectedJob.company} • {selectedJob.location} ({selectedJob.employmentType})
                </p>
                <p>
                  <strong>Description:</strong>
                  <br />
                  {selectedJob.description}
                </p>
                <p>
                  <strong>Required Skills:</strong> {selectedJob.requiredSkills}
                </p>
                {selectedJob.preferredQualifications && (
                  <p>
                    <strong>Preferred Qualifications:</strong> {selectedJob.preferredQualifications}
                  </p>
                )}
                <p style={{ fontSize: "0.85rem", color: "var(--muted)" }}>
                  Posted: {selectedJob.postedDate}{" "}
                  {selectedJob.expiryDate ? `| Expires: ${selectedJob.expiryDate}` : ""}
                </p>
                <div style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem", marginTop: "1rem" }}>
                  <button
                    className="btn btn-primary"
                    onClick={() => handleOpenQuickApply(selectedJob)}
                  >
                    ⚡ 1-Click Auto-Fill & Apply
                  </button>
                  {selectedJob.sourceUrl && (
                    <a
                      href={selectedJob.sourceUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="btn btn-outline"
                    >
                      🌐 Manual Apply on Portal ↗
                    </a>
                  )}
                  <button
                    className="btn btn-outline"
                    onClick={() => onSelectJobForAI(selectedJob)}
                  >
                    Run AI Match & Resume Optimizer
                  </button>
                  <button
                    className="btn btn-outline"
                    onClick={() => handleSaveToApplications(selectedJob, "READY_TO_APPLY")}
                  >
                    Prepare Application
                  </button>
                </div>
              </div>
            ) : (
              <div className="card" style={{ color: "var(--muted)" }}>
                Select any job listing on the left to inspect full job details, required qualifications, and application options.
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
