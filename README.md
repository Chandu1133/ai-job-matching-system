# AI-Powered Job Matching System

An end-to-end **AI-Powered Job Matching, Truthful Resume Optimization, and Application Tracking Platform** built with **Java 25 & Spring Boot 4**, **MySQL**, **Python FastAPI (NLP Service)**, and **React (Vite)**.

---

## 1. Project Purpose & Key Features

1. **Secure Authentication & Session Management**:
   - Candidate registration and login with BCrypt password hashing (`BCryptPasswordEncoder`), stateful HTTP session management, session-fixation protection, and cookie-based CSRF protection (`CookieCsrfTokenRepository`).
2. **Candidate Profile Management**:
   - Manage skills, education, work experience years, location, phone, and preferred job roles (`GET /api/users/me`, `PUT /api/users/me`) with strict ownership isolation.
3. **Job Discovery, Filtering & Bookmarks**:
   - Dynamic multi-criteria search by keyword, title, company, location, employment type, and experience level with sorting and pagination (`GET /api/jobs`).
   - Duplicate job detection (`409 Conflict`), expired job handling, realistic seed data marked with `seedData = true`, and job bookmarking (`/api/jobs/{id}/bookmark`).
4. **Secure Resume Management & Parsing**:
   - Upload `.pdf` and `.docx` resumes (validated up to 5 MB via extension + binary magic bytes), stored outside the public web root with UUID filenames.
   - Extracts resume text, skills, education, work experience, and projects, and provides a human-in-the-loop review endpoint (`PUT /api/resumes/{id}/review`).
5. **Explainable AI/NLP Job Matching (FastAPI + Spring Boot)**:
   - Normalizes skill synonyms (e.g., `springboot` -> `Spring Boot`, `reactjs` -> `React`) and computes a weighted 0–100 alignment score (65% required skills, 20% experience alignment, 15% cosine text similarity) with a clear explanation of matched and missing skills.
   - Includes a 3-second timeout and deterministic fallback in Spring Boot if the FastAPI service is offline.
6. **Truthful Resume Optimization**:
   - Compares a candidate's verified profile and resume against a target job, highlights supported keywords vs. missing skills, generates a tailored preview **without fabricating unverified qualifications**, and requires candidate approval before saving (`POST /api/optimizations/{id}/approve`).
7. **Application Tracking, Assistance & Dashboard Analytics**:
   - Tracks statuses (`SAVED`, `READY_TO_APPLY`, `APPLIED`, `INTERVIEW`, `REJECTED`, `OFFER`) with full transition history (`statusHistory`), notes, and follow-up dates.
   - Generates prefilled application assistance packets (`GET /api/applications/assist/job/{jobId}`) and requires explicit submission confirmation (`POST /api/applications/{id}/confirm-submission`) before marking an application as `APPLIED`.

---

## 2. System Architecture & Directory Structure

```
job-matching-system/
├── pom.xml                                  # Maven build configuration (Java 25, Spring Boot 4.1.1)
├── .env.example                             # Environment variable template (no real secrets)
├── src/main/
│   ├── java/com/chandu/job_matching_system/
│   │   ├── config/                          # SecurityConfig, JobDataSeeder
│   │   ├── controller/                      # REST API Controllers (Auth, UserProfile, Job, Resume, Match, Optimization, Application)
│   │   ├── service/                         # Business Logic & ResumeParserService
│   │   ├── repository/                      # Spring Data JPA Repositories
│   │   ├── entity/                          # JPA Entities (User, Job, SavedJob, Resume, JobMatch, ResumeOptimization, JobApplication)
│   │   ├── dto/                             # Request & Response DTOs with Jakarta Bean Validation
│   │   └── exception/                       # GlobalExceptionHandler
│   └── resources/
│       └── application.properties.example   # Example Spring Boot configuration
├── src/test/
│   ├── java/com/chandu/job_matching_system/ # JUnit 5 & Mockito unit + integration tests
│   └── resources/application.properties     # H2 in-memory test database configuration
├── nlp-service/                             # Python FastAPI AI/NLP Microservice
│   ├── main.py                              # FastAPI application (/health, /api/nlp/match)
│   ├── matching_engine.py                   # Skill normalization, cosine similarity & weighted scoring
│   ├── test_matching.py                     # Python unit test suite
│   └── requirements.txt                     # Python dependencies
└── frontend/                                # React (Vite) Single Page Application
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── services/api.js                  # Centralized API client with CSRF & session support
        ├── pages/                           # Auth, Dashboard, Profile, Jobs, Resumes, Matching, Applications
        └── App.jsx
```

---

## 3. Prerequisites

- **Java**: JDK 25
- **Build Tool**: Maven (or included `./mvnw` / `.\mvnw.cmd` wrapper)
- **Database**: MySQL 8.0+
- **Python**: Python 3.10+ (for `nlp-service`)
- **Node.js**: Node.js 18+ and npm (for `frontend`)

---

## 4. Setup & Running Instructions

### Step 1: MySQL Database Setup
Create the database in MySQL:
```sql
CREATE DATABASE IF NOT EXISTS job_matching_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
Copy `src/main/resources/application.properties.example` to `src/main/resources/application.properties` and set your MySQL username and password (or set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` environment variables).

### Step 2: Start the Python FastAPI NLP Service
```powershell
cd nlp-service
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
- Health check: `GET http://localhost:8000/health`

### Step 3: Start the Spring Boot Backend
From the project root:
```powershell
.\mvnw.cmd spring-boot:run
```
- The backend runs on `http://localhost:8080`.

### Step 4: Start the React Frontend
```powershell
cd frontend
npm install
npm run dev
```
- Open `http://localhost:5173` in your browser.

---

## 5. Running Automated Tests

- **Python NLP Unit Tests**:
  ```powershell
  python nlp-service\test_matching.py -v
  ```
- **Java Compilation & Test Verification**:
  ```powershell
  .\mvnw.cmd test-compile
  ```
  *(To run JUnit tests from your IDE or terminal, run `AuthServiceTest`, `UserProfileServiceTest`, `JobServiceTest`, `ResumeServiceTest`, `JobMatchingServiceTest`, `ResumeOptimizationServiceTest`, `JobApplicationServiceTest`, and `AuthAndProfileIntegrationTest`—all configured to use the in-memory H2 database in `src/test/resources/application.properties`.)*

---

## 6. Current Limitations & Future Improvements

- **Current Limitations**:
  - PDF text extraction uses built-in stream decompression and literal parsing; scanned image-only PDFs require OCR or manual review via the Candidate Review form (`PUT /api/resumes/{id}/review`).
  - External employer portals without public APIs require the candidate to submit via the official link using the generated Prefill Assistance Packet and then confirm submission.
- **Future Improvements**:
  - Integrate transformer-based sentence embeddings (e.g., `sentence-transformers`) in `nlp-service` for deeper contextual matching.
  - Add downloadable PDF/DOCX export for approved resume optimizations.
  - Add email/calendar notifications for upcoming application follow-up dates.
