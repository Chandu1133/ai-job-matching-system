import React, { useEffect, useState } from "react";
import { api } from "../services/api.js";

export default function ProfilePage() {
  const [profile, setProfile] = useState({
    name: "",
    email: "",
    phone: "",
    location: "",
    education: "",
    skills: "",
    preferredJobRole: "",
    experienceYears: 0
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    loadProfile();
  }, []);

  async function loadProfile() {
    setLoading(true);
    try {
      const data = await api.getProfile();
      setProfile({
        name: data.name || "",
        email: data.email || "",
        phone: data.phone || "",
        location: data.location || "",
        education: data.education || "",
        skills: data.skills || "",
        preferredJobRole: data.preferredJobRole || "",
        experienceYears: data.experienceYears ?? 0
      });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleSave(e) {
    e.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const updated = await api.updateProfile({
        name: profile.name,
        phone: profile.phone,
        location: profile.location,
        education: profile.education,
        skills: profile.skills,
        preferredJobRole: profile.preferredJobRole,
        experienceYears: Number(profile.experienceYears || 0)
      });
      setSuccess(updated.message || "Profile updated successfully!");
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <div className="card">Loading candidate profile...</div>;

  return (
    <div className="card">
      <h2 style={{ marginTop: 0 }}>Candidate Profile</h2>
      <p style={{ color: "var(--muted)", fontSize: "0.9rem" }}>
        Your verified profile details are used for AI job matching, truthful resume optimization, and application prefill assistance.
      </p>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <form onSubmit={handleSave}>
        <div className="grid-2">
          <div className="form-group">
            <label>Full Name *</label>
            <input
              type="text"
              className="form-control"
              value={profile.name}
              onChange={(e) => setProfile({ ...profile, name: e.target.value })}
              required
            />
          </div>
          <div className="form-group">
            <label>Email Address (Read-only)</label>
            <input type="email" className="form-control" value={profile.email} disabled />
          </div>
          <div className="form-group">
            <label>Phone Number</label>
            <input
              type="text"
              className="form-control"
              value={profile.phone}
              onChange={(e) => setProfile({ ...profile, phone: e.target.value })}
              placeholder="+91-9876543210"
            />
          </div>
          <div className="form-group">
            <label>Location</label>
            <input
              type="text"
              className="form-control"
              value={profile.location}
              onChange={(e) => setProfile({ ...profile, location: e.target.value })}
              placeholder="Hyderabad, India"
            />
          </div>
          <div className="form-group">
            <label>Preferred Job Role</label>
            <input
              type="text"
              className="form-control"
              value={profile.preferredJobRole}
              onChange={(e) => setProfile({ ...profile, preferredJobRole: e.target.value })}
              placeholder="Java Full Stack Developer"
            />
          </div>
          <div className="form-group">
            <label>Years of Experience</label>
            <input
              type="number"
              min="0"
              max="60"
              className="form-control"
              value={profile.experienceYears}
              onChange={(e) => setProfile({ ...profile, experienceYears: e.target.value })}
            />
          </div>
        </div>

        <div className="form-group">
          <label>Education</label>
          <input
            type="text"
            className="form-control"
            value={profile.education}
            onChange={(e) => setProfile({ ...profile, education: e.target.value })}
            placeholder="B.Tech in Computer Science and Engineering"
          />
        </div>

        <div className="form-group">
          <label>Verified Technical Skills (Comma-separated)</label>
          <textarea
            className="form-control"
            value={profile.skills}
            onChange={(e) => setProfile({ ...profile, skills: e.target.value })}
            placeholder="Java, Spring Boot, MySQL, Python, FastAPI, React, Git"
          />
        </div>

        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? "Saving..." : "Save Profile"}
        </button>
      </form>
    </div>
  );
}
