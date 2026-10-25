import React, { useState } from "react";
import { api } from "../services/api.js";

export default function AuthPage({ onAuthenticated }) {
  const [mode, setMode] = useState("login");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSuccessMsg("");

    if (!email.trim() || !password) {
      setError("Email and password are required.");
      return;
    }
    if (mode === "register" && (!name.trim() || password.length < 8)) {
      setError("Name is required and password must be at least 8 characters.");
      return;
    }

    setLoading(true);
    try {
      if (mode === "register") {
        const authData = await api.register({
          name: name.trim(),
          email: email.trim(),
          password
        });
        setSuccessMsg("Registration successful! Logging you in...");
        onAuthenticated(authData);
      } else {
        const authData = await api.login({ email: email.trim(), password });
        onAuthenticated(authData);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleResetPassword() {
    setError("");
    setSuccessMsg("");
    if (!email.trim() || password.trim().length < 8) {
      setError("Enter your registered email and a password of at least 8 characters.");
      return;
    }
    setLoading(true);
    try {
      const authData = await api.resetPassword({
        email: email.trim(),
        password: password.trim()
      });
      setSuccessMsg("Password updated in MySQL! Logging you in...");
      onAuthenticated(authData);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="container" style={{ maxWidth: 460, marginTop: "3rem" }}>
      <div className="card">
        <h2 style={{ marginTop: 0 }}>
          {mode === "login" ? "Candidate Sign In" : "Create Candidate Account"}
        </h2>
        <p style={{ color: "var(--muted)", fontSize: "0.92rem" }}>
          AI-Powered Job Matching, Truthful Resume Optimization & Application Tracking
        </p>

        {error && <div className="alert alert-error">{error}</div>}
        {successMsg && <div className="alert alert-success">{successMsg}</div>}

        <form onSubmit={handleSubmit}>
          {mode === "register" && (
            <div className="form-group">
              <label>Full Name</label>
              <input
                type="text"
                className="form-control"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Chandu Kumar"
                required
              />
            </div>
          )}

          <div className="form-group">
            <label>Email Address</label>
            <input
              type="email"
              className="form-control"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="candidate@example.com"
              required
            />
          </div>

          <div className="form-group">
            <label>Password</label>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <input
                type={showPassword ? "text" : "password"}
                className="form-control"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder={mode === "register" ? "8 to 72 characters" : "Enter password"}
                required
              />
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => setShowPassword((prev) => !prev)}
                style={{ whiteSpace: "nowrap" }}
              >
                {showPassword ? "Hide" : "Show"}
              </button>
            </div>
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: "100%" }} disabled={loading}>
            {loading ? "Processing..." : mode === "login" ? "Sign In" : "Register & Sign In"}
          </button>

          {mode === "login" && error && (
            <button
              type="button"
              className="btn btn-outline"
              style={{ width: "100%", marginTop: "0.6rem" }}
              onClick={handleResetPassword}
              disabled={loading}
            >
              Update MySQL Password to This & Sign In
            </button>
          )}
        </form>

        <div style={{ marginTop: "1rem", textAlign: "center", fontSize: "0.9rem" }}>
          {mode === "login" ? (
            <>
              Don&apos;t have an account?{" "}
              <button
                type="button"
                className="btn btn-outline"
                style={{ padding: "0.25rem 0.6rem" }}
                onClick={() => {
                  setMode("register");
                  setError("");
                }}
              >
                Register
              </button>
            </>
          ) : (
            <>
              Already registered?{" "}
              <button
                type="button"
                className="btn btn-outline"
                style={{ padding: "0.25rem 0.6rem" }}
                onClick={() => {
                  setMode("login");
                  setError("");
                }}
              >
                Sign In
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
