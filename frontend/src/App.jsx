import React, { useEffect, useState } from "react";
import { api, setStoredUser } from "./services/api.js";
import AuthPage from "./pages/AuthPage.jsx";
import DashboardPage from "./pages/DashboardPage.jsx";
import ProfilePage from "./pages/ProfilePage.jsx";
import JobsPage from "./pages/JobsPage.jsx";
import ResumesPage from "./pages/ResumesPage.jsx";
import MatchingAndOptimizationPage from "./pages/MatchingAndOptimizationPage.jsx";
import ApplicationsPage from "./pages/ApplicationsPage.jsx";

export default function App() {
  const [user, setUser] = useState(null);
  const [checkingSession, setCheckingSession] = useState(true);
  const [activeTab, setActiveTab] = useState("dashboard");
  const [selectedJobForAI, setSelectedJobForAI] = useState(null);

  useEffect(() => {
    checkExistingSession();
  }, []);

  async function checkExistingSession() {
    setCheckingSession(true);
    try {
      const profile = await api.getProfile();
      setStoredUser(profile);
      setUser(profile);
    } catch {
      setStoredUser(null);
      setUser(null);
    } finally {
      setCheckingSession(false);
    }
  }

  async function handleLogout() {
    try {
      await api.logout();
    } finally {
      setStoredUser(null);
      setUser(null);
      setActiveTab("dashboard");
    }
  }

  function handleSelectJobForAI(job) {
    setSelectedJobForAI(job);
    setActiveTab("matching");
  }

  if (checkingSession) {
    return <div className="container">Checking session...</div>;
  }

  if (!user) {
    return (
      <AuthPage
        onAuthenticated={(authData) => {
          setStoredUser(authData);
          setUser(authData);
        }}
      />
    );
  }

  return (
    <div className="app-layout">
      <header className="navbar">
        <div className="navbar-brand">AI Job Matching Platform</div>
        <nav className="nav-links">
          <button
            className={`nav-btn ${activeTab === "dashboard" ? "active" : ""}`}
            onClick={() => setActiveTab("dashboard")}
          >
            Dashboard
          </button>
          <button
            className={`nav-btn ${activeTab === "jobs" ? "active" : ""}`}
            onClick={() => setActiveTab("jobs")}
          >
            Job Search
          </button>
          <button
            className={`nav-btn ${activeTab === "saved" ? "active" : ""}`}
            onClick={() => setActiveTab("saved")}
          >
            Saved Jobs
          </button>
          <button
            className={`nav-btn ${activeTab === "resumes" ? "active" : ""}`}
            onClick={() => setActiveTab("resumes")}
          >
            Resumes
          </button>
          <button
            className={`nav-btn ${activeTab === "matching" ? "active" : ""}`}
            onClick={() => setActiveTab("matching")}
          >
            AI Match & Optimize
          </button>
          <button
            className={`nav-btn ${activeTab === "applications" ? "active" : ""}`}
            onClick={() => setActiveTab("applications")}
          >
            Applications
          </button>
          <button
            className={`nav-btn ${activeTab === "profile" ? "active" : ""}`}
            onClick={() => setActiveTab("profile")}
          >
            Profile ({user.name})
          </button>
          <button className="nav-btn" onClick={handleLogout}>
            Logout
          </button>
        </nav>
      </header>

      <main className="container">
        {activeTab === "dashboard" && <DashboardPage onNavigate={setActiveTab} />}
        {activeTab === "profile" && <ProfilePage />}
        {activeTab === "jobs" && (
          <JobsPage onSelectJobForAI={handleSelectJobForAI} savedOnly={false} />
        )}
        {activeTab === "saved" && (
          <JobsPage onSelectJobForAI={handleSelectJobForAI} savedOnly={true} />
        )}
        {activeTab === "resumes" && <ResumesPage />}
        {activeTab === "matching" && (
          <MatchingAndOptimizationPage initialJob={selectedJobForAI} />
        )}
        {activeTab === "applications" && <ApplicationsPage />}
      </main>
    </div>
  );
}
