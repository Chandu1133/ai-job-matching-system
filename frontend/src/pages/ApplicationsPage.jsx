import React, { useEffect, useState } from "react";
import { api } from "../services/api.js";

export default function ApplicationsPage() {
  const [applications, setApplications] = useState([]);
  const [statusFilter, setStatusFilter] = useState("");
  const [selectedApp, setSelectedApp] = useState(null);
  const [prefillPacket, setPrefillPacket] = useState(null);
  const [confirmationRef, setConfirmationRef] = useState("");
  const [statusToUpdate, setStatusToUpdate] = useState("INTERVIEW");
  const [notes, setNotes] = useState("");
  const [followUpDate, setFollowUpDate] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    loadApplications();
  }, [statusFilter]);

  async function loadApplications() {
    setLoading(true);
    setError("");
    try {
      const list = await api.getApplications({ status: statusFilter });
      setApplications(list || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleInspectApplication(app) {
    setSelectedApp(app);
    setNotes(app.notes || "");
    setFollowUpDate(app.followUpDate || "");
    setError("");
    try {
      const prefill = await api.getPrefillAssistance(app.jobId, app.resumeId);
      setPrefillPacket(prefill);
    } catch {
      setPrefillPacket(null);
    }
  }

  async function handleConfirmSubmission(e) {
    e.preventDefault();
    if (!selectedApp || !confirmationRef.trim()) {
      setError("Please enter your submission confirmation details to mark as APPLIED.");
      return;
    }
    setError("");
    setSuccess("");
    try {
      const updated = await api.confirmSubmission(selectedApp.id, {
        confirmationDetails: confirmationRef.trim(),
        notes
      });
      setSuccess(updated.message);
      setSelectedApp(updated);
      setConfirmationRef("");
      await loadApplications();
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleStatusUpdate(e) {
    e.preventDefault();
    if (!selectedApp) return;
    setError("");
    setSuccess("");
    try {
      const updated = await api.updateApplicationStatus(selectedApp.id, {
        status: statusToUpdate,
        followUpDate: followUpDate || null,
        notes,
        submissionConfirmation: confirmationRef || undefined
      });
      setSuccess(updated.message);
      setSelectedApp(updated);
      await loadApplications();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div>
      <div className="flex-between" style={{ marginBottom: "1rem" }}>
        <h2 style={{ margin: 0 }}>Application Tracker & Assistance</h2>
        <select
          className="form-control"
          style={{ width: 220 }}
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="">All Statuses</option>
          <option value="SAVED">SAVED</option>
          <option value="READY_TO_APPLY">READY_TO_APPLY</option>
          <option value="APPLIED">APPLIED</option>
          <option value="INTERVIEW">INTERVIEW</option>
          <option value="OFFER">OFFER</option>
          <option value="REJECTED">REJECTED</option>
        </select>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="grid-2">
        <div>
          {loading ? (
            <div className="card">Loading applications...</div>
          ) : applications.length === 0 ? (
            <div className="card">No applications match the current filter.</div>
          ) : (
            applications.map((app) => (
              <div key={app.id} className="card">
                <div className="flex-between">
                  <div>
                    <h3 style={{ margin: 0 }}>{app.jobTitle}</h3>
                    <div style={{ color: "var(--muted)", fontSize: "0.9rem" }}>
                      {app.company} • {app.location}
                    </div>
                  </div>
                  <span className="badge badge-info">{app.status}</span>
                </div>
                <div style={{ fontSize: "0.85rem", color: "var(--muted)", marginTop: "0.5rem" }}>
                  Applied Date: {app.applicationDate || "Not submitted yet"} | Follow-Up:{" "}
                  {app.followUpDate || "None"}
                </div>
                <div style={{ marginTop: "0.75rem" }}>
                  <button
                    className="btn btn-outline"
                    onClick={() => handleInspectApplication(app)}
                  >
                    Manage / Prefill Assistance / History
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        <div>
          {selectedApp ? (
            <div className="card">
              <h3 style={{ marginTop: 0 }}>
                {selectedApp.jobTitle} — {selectedApp.company}
              </h3>
              <p>
                <strong>Current Status:</strong>{" "}
                <span className="badge badge-success">{selectedApp.status}</span>
              </p>

              {prefillPacket && (
                <div style={{ marginBottom: "1.25rem" }}>
                  <h4>Application Assistance (Verified Prefill Packet)</h4>
                  <p style={{ fontSize: "0.86rem", color: "var(--muted)" }}>
                    {prefillPacket.workflowGuidance}
                  </p>
                  <div className="pre-box">
                    {`Candidate: ${prefillPacket.candidateName} (${prefillPacket.candidateEmail})\nPhone: ${prefillPacket.candidatePhone || "N/A"} | Location: ${prefillPacket.candidateLocation || "N/A"}\nVerified Skills: ${prefillPacket.verifiedSkills || "N/A"}\nResume: ${prefillPacket.selectedResumeFileName || "Profile Only"}\n\n--- Draft Cover Note ---\n${prefillPacket.tailoredCoverNote}`}
                  </div>
                  {prefillPacket.directApplicationUrl &&
                    prefillPacket.directApplicationUrl.startsWith("http") && (
                      <div style={{ marginTop: "0.5rem" }}>
                        <a
                          href={prefillPacket.directApplicationUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="btn btn-outline"
                        >
                          Open Official Job Portal ↗
                        </a>
                      </div>
                    )}
                </div>
              )}

              {selectedApp.status !== "APPLIED" && (
                <form
                  onSubmit={handleConfirmSubmission}
                  style={{
                    borderTop: "1px solid var(--border)",
                    paddingTop: "1rem",
                    marginBottom: "1rem"
                  }}
                >
                  <h4>Record Verified Submission (Transition to APPLIED)</h4>
                  <div className="form-group">
                    <label>Submission Confirmation Reference / Details *</label>
                    <input
                      className="form-control"
                      placeholder="e.g., Submitted on company portal, Confirmation #1024"
                      value={confirmationRef}
                      onChange={(e) => setConfirmationRef(e.target.value)}
                      required
                    />
                  </div>
                  <button type="submit" className="btn btn-primary">
                    Confirm Submission & Mark APPLIED
                  </button>
                </form>
              )}

              <form
                onSubmit={handleStatusUpdate}
                style={{ borderTop: "1px solid var(--border)", paddingTop: "1rem" }}
              >
                <h4>Update Status / Follow-Up / Notes</h4>
                <div className="grid-2">
                  <div className="form-group">
                    <label>Status</label>
                    <select
                      className="form-control"
                      value={statusToUpdate}
                      onChange={(e) => setStatusToUpdate(e.target.value)}
                    >
                      <option value="READY_TO_APPLY">READY_TO_APPLY</option>
                      <option value="INTERVIEW">INTERVIEW</option>
                      <option value="OFFER">OFFER</option>
                      <option value="REJECTED">REJECTED</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Follow-Up Date</label>
                    <input
                      type="date"
                      className="form-control"
                      value={followUpDate}
                      onChange={(e) => setFollowUpDate(e.target.value)}
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label>Notes</label>
                  <textarea
                    className="form-control"
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                  />
                </div>
                <button type="submit" className="btn btn-outline">
                  Update Application
                </button>
              </form>

              <div style={{ marginTop: "1.25rem" }}>
                <h4>Status History Timeline</h4>
                <ul>
                  {(selectedApp.statusHistory || []).map((entry, idx) => (
                    <li key={idx} style={{ fontSize: "0.85rem" }}>
                      {entry}
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          ) : (
            <div className="card" style={{ color: "var(--muted)" }}>
              Select an application record on the left to view prefilled application assistance, confirm portal submission, or update status history.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
