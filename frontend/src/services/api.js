const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/+$/, "");
const AUTH_STORAGE_KEY = "job_matching_auth_user";

export function getStoredUser() {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setStoredUser(user) {
  try {
    if (user && user.email) {
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(AUTH_STORAGE_KEY);
    }
  } catch {
    // Ignore storage errors
  }
}

function getCookieValue(name) {
  const match = document.cookie.match(new RegExp("(^| )" + name + "=([^;]+)"));
  return match ? decodeURIComponent(match[2]) : null;
}

export async function ensureCsrfToken() {
  let token = getCookieValue("XSRF-TOKEN");
  if (!token) {
    const res = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
      method: "GET",
      credentials: "include"
    });
    if (res.ok) {
      const data = await res.json().catch(() => null);
      token = getCookieValue("XSRF-TOKEN") || (data && data.token);
    }
  }
  return token;
}

export async function apiRequest(path, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  const headers = { ...(options.headers || {}) };

  const storedUser = getStoredUser();
  if (storedUser?.email) {
    headers["X-Candidate-Email"] = storedUser.email;
  }

  if (!(options.body instanceof FormData) && !headers["Content-Type"] && method !== "GET") {
    headers["Content-Type"] = "application/json";
  }

  if (["POST", "PUT", "PATCH", "DELETE"].includes(method)) {
    const csrfToken = await ensureCsrfToken();
    if (csrfToken) {
      headers["X-XSRF-TOKEN"] = csrfToken;
    }
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    method,
    headers,
    credentials: "include"
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const validationErrors = data?.validationErrors
      ? Object.values(data.validationErrors).join(", ")
      : null;
    const errorMessage =
      validationErrors ||
      data?.message ||
      data?.error ||
      `Request failed with status ${response.status}`;
    const err = new Error(errorMessage);
    err.status = response.status;
    err.data = data;
    throw err;
  }

  return data;
}

export const api = {
  // Authentication
  register: async (payload) => {
    const data = await apiRequest("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    setStoredUser(data);
    return data;
  },
  login: async (payload) => {
    const data = await apiRequest("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    setStoredUser(data);
    return data;
  },
  resetPassword: async (payload) => {
    const data = await apiRequest("/api/auth/reset-password", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    setStoredUser(data);
    return data;
  },
  logout: async () => {
    try {
      return await apiRequest("/api/auth/logout", { method: "POST" });
    } finally {
      setStoredUser(null);
    }
  },

  // Candidate Profile
  getProfile: () => apiRequest("/api/users/me"),
  updateProfile: (payload) =>
    apiRequest("/api/users/me", { method: "PUT", body: JSON.stringify(payload) }),

  // Jobs & Bookmarks
  searchJobs: (params = {}) => {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== "") query.append(k, v);
    });
    return apiRequest(`/api/jobs?${query.toString()}`);
  },
  getJobById: (id) => apiRequest(`/api/jobs/${id}`),
  createJob: (payload) =>
    apiRequest("/api/jobs", { method: "POST", body: JSON.stringify(payload) }),
  syncExternalJobs: ({ query, apiKey } = {}) => {
    const params = new URLSearchParams();
    if (query) params.append("query", query);
    if (apiKey) params.append("apiKey", apiKey);
    return apiRequest(`/api/jobs/sync-external?${params.toString()}`, { method: "POST" });
  },
  bookmarkJob: (id) => apiRequest(`/api/jobs/${id}/bookmark`, { method: "POST" }),
  removeBookmark: (id) => apiRequest(`/api/jobs/${id}/bookmark`, { method: "DELETE" }),
  getBookmarkedJobs: () => apiRequest("/api/jobs/bookmarks"),

  // Resumes
  uploadResume: (file) => {
    const formData = new FormData();
    formData.append("file", file);
    return apiRequest("/api/resumes/upload", { method: "POST", body: formData });
  },
  getResumes: () => apiRequest("/api/resumes"),
  reviewResume: (id, payload) =>
    apiRequest(`/api/resumes/${id}/review`, { method: "PUT", body: JSON.stringify(payload) }),
  deleteResume: (id) => apiRequest(`/api/resumes/${id}`, { method: "DELETE" }),

  // AI/NLP Job Matching
  calculateMatch: (jobId, resumeId) => {
    const query = resumeId ? `?resumeId=${resumeId}` : "";
    return apiRequest(`/api/matches/job/${jobId}${query}`, { method: "POST" });
  },
  getMyMatches: () => apiRequest("/api/matches"),

  // Truthful Resume Optimization
  generateOptimization: (resumeId, jobId) =>
    apiRequest(`/api/optimizations/resume/${resumeId}/job/${jobId}`, { method: "POST" }),
  approveOptimization: (id, payload) =>
    apiRequest(`/api/optimizations/${id}/approve`, { method: "POST", body: JSON.stringify(payload) }),
  getMyOptimizations: () => apiRequest("/api/optimizations"),
  deleteOptimization: (id) =>
    apiRequest(`/api/optimizations/${id}`, { method: "DELETE" }),

  // Application Management & Assistance
  getDashboardSummary: () => apiRequest("/api/applications/dashboard"),
  getPrefillAssistance: (jobId, resumeId) => {
    const query = resumeId ? `?resumeId=${resumeId}` : "";
    return apiRequest(`/api/applications/assist/job/${jobId}${query}`);
  },
  createApplication: (payload) =>
    apiRequest("/api/applications", { method: "POST", body: JSON.stringify(payload) }),
  confirmSubmission: (id, payload) =>
    apiRequest(`/api/applications/${id}/confirm-submission`, {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  updateApplicationStatus: (id, payload) =>
    apiRequest(`/api/applications/${id}/status`, {
      method: "PUT",
      body: JSON.stringify(payload)
    }),
  getApplications: (params = {}) => {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== "") query.append(k, v);
    });
    return apiRequest(`/api/applications?${query.toString()}`);
  }
};
