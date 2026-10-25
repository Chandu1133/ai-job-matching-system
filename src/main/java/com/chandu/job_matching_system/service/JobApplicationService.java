package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.*;
import com.chandu.job_matching_system.entity.*;
import com.chandu.job_matching_system.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class JobApplicationService {

    public static final Set<String> ALLOWED_STATUSES = Set.of(
            "SAVED",
            "READY_TO_APPLY",
            "APPLIED",
            "INTERVIEW",
            "REJECTED",
            "OFFER"
    );

    private final JobApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;
    private final SavedJobRepository savedJobRepository;
    private final JobMatchRepository jobMatchRepository;
    private final UserRepository userRepository;

    public JobApplicationService(
            JobApplicationRepository applicationRepository,
            JobRepository jobRepository,
            ResumeRepository resumeRepository,
            SavedJobRepository savedJobRepository,
            JobMatchRepository jobMatchRepository,
            UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
        this.savedJobRepository = savedJobRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ApplicationPrefillPreviewResponse generatePrefillPreview(
            Long jobId,
            Long resumeId,
            String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job listing not found"
                ));

        Resume resume = resolveResume(user, resumeId);
        String verifiedSkills = combineCsv(
                user.getSkills(),
                resume != null ? resume.getExtractedSkills() : null
        );
        String verifiedEducation = nonBlank(
                resume != null ? resume.getExtractedEducation() : null,
                user.getEducation()
        );

        String coverNote = buildCoverNote(user, job, verifiedSkills);
        String directUrl = job.getSourceUrl() != null && !job.getSourceUrl().isBlank()
                ? job.getSourceUrl()
                : "Direct portal URL not provided by employer";

        String guidance = "External employer portals require candidate review and direct submission. "
                + "Review your prefilled verified profile packet below, submit on the official job link, "
                + "and then confirm submission with your reference details to mark the record as APPLIED.";

        return new ApplicationPrefillPreviewResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                directUrl,
                false,
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getLocation(),
                verifiedEducation,
                verifiedSkills,
                user.getExperienceYears(),
                resume != null ? resume.getId() : null,
                resume != null ? resume.getOriginalFileName() : null,
                coverNote,
                guidance
        );
    }

    @Transactional
    public JobApplicationResponse createApplication(
            ApplicationCreateRequest request,
            String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job listing not found"
                ));

        if (job.isExpired()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot create an active application for an expired or unavailable job listing"
            );
        }

        if (applicationRepository.existsByUserIdAndJobId(user.getId(), job.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An application record for this job already exists"
            );
        }

        String requestedStatus = normalizeStatus(request.getStatus());
        if ("APPLIED".equals(requestedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New applications must start as SAVED or READY_TO_APPLY. Use the confirm-submission workflow after submitting."
            );
        }

        Resume resume = resolveResume(user, request.getResumeId());
        ApplicationPrefillPreviewResponse prefill = generatePrefillPreview(
                job.getId(),
                resume != null ? resume.getId() : null,
                authenticatedEmail
        );

        JobApplication app = new JobApplication();
        app.setUser(user);
        app.setJob(job);
        app.setResume(resume);
        app.setStatus(requestedStatus);
        app.setFollowUpDate(request.getFollowUpDate());
        app.setJobSource(job.getSourceUrl());
        app.setNotes(trimToNull(request.getNotes()));
        app.setPrefilledDataPreview(formatPrefillText(prefill));
        app.setCreatedAt(Instant.now());
        app.setUpdatedAt(Instant.now());
        appendHistory(app, "CREATED -> " + requestedStatus, "Application record initialized");

        JobApplication saved = applicationRepository.save(app);
        return toResponse(saved, "Application record created with status " + requestedStatus);
    }

    @Transactional
    public JobApplicationResponse confirmSubmission(
            Long applicationId,
            ApplicationSubmissionConfirmRequest request,
            String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);
        JobApplication app = applicationRepository.findByIdAndUserId(applicationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application record not found"
                ));

        String previousStatus = app.getStatus();
        Instant now = Instant.now();

        app.setStatus("APPLIED");
        app.setApplicationDate(LocalDate.now());
        app.setSubmittedAt(now);
        app.setSubmissionConfirmation(request.getConfirmationDetails().trim());
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            app.setNotes(request.getNotes().trim());
        }
        app.setUpdatedAt(now);

        appendHistory(
                app,
                previousStatus + " -> APPLIED",
                "Confirmed submission: " + request.getConfirmationDetails().trim()
        );

        JobApplication saved = applicationRepository.save(app);
        return toResponse(saved, "Application submission confirmed and recorded as APPLIED");
    }

    @Transactional
    public JobApplicationResponse updateStatus(
            Long applicationId,
            ApplicationStatusUpdateRequest request,
            String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);
        JobApplication app = applicationRepository.findByIdAndUserId(applicationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application record not found"
                ));

        String newStatus = normalizeStatus(request.getStatus());
        if ("APPLIED".equals(newStatus)
                && (request.getSubmissionConfirmation() == null || request.getSubmissionConfirmation().isBlank())
                && app.getSubmissionConfirmation() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Marking an application as APPLIED requires submission confirmation details"
            );
        }

        String oldStatus = app.getStatus();
        app.setStatus(newStatus);
        if (request.getFollowUpDate() != null) {
            app.setFollowUpDate(request.getFollowUpDate());
        }
        if (request.getNotes() != null) {
            app.setNotes(trimToNull(request.getNotes()));
        }
        if ("APPLIED".equals(newStatus)) {
            if (app.getApplicationDate() == null) {
                app.setApplicationDate(LocalDate.now());
            }
            if (app.getSubmittedAt() == null) {
                app.setSubmittedAt(Instant.now());
            }
            if (request.getSubmissionConfirmation() != null && !request.getSubmissionConfirmation().isBlank()) {
                app.setSubmissionConfirmation(request.getSubmissionConfirmation().trim());
            }
        }
        app.setUpdatedAt(Instant.now());

        if (!oldStatus.equals(newStatus)) {
            appendHistory(
                    app,
                    oldStatus + " -> " + newStatus,
                    request.getNotes() != null && !request.getNotes().isBlank()
                            ? request.getNotes().trim()
                            : "Status updated"
            );
        }

        JobApplication saved = applicationRepository.save(app);
        return toResponse(saved, "Application status updated to " + newStatus);
    }

    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getMyApplications(
            String status,
            LocalDate fromDate,
            LocalDate toDate,
            String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);
        List<JobApplication> all = applicationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());

        String normalizedStatusFilter = (status != null && !status.isBlank())
                ? status.trim().toUpperCase(Locale.ROOT)
                : null;

        return all.stream()
                .filter(app -> normalizedStatusFilter == null || normalizedStatusFilter.equals(app.getStatus()))
                .filter(app -> {
                    if (fromDate == null && toDate == null) {
                        return true;
                    }
                    LocalDate refDate = app.getApplicationDate() != null
                            ? app.getApplicationDate()
                            : LocalDate.ofInstant(app.getCreatedAt(), java.time.ZoneOffset.UTC);
                    if (fromDate != null && refDate.isBefore(fromDate)) {
                        return false;
                    }
                    if (toDate != null && refDate.isAfter(toDate)) {
                        return false;
                    }
                    return true;
                })
                .map(app -> toResponse(app, "Application retrieved"))
                .toList();
    }

    @Transactional(readOnly = true)
    public JobApplicationResponse getApplicationById(Long id, String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        JobApplication app = applicationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application record not found"
                ));
        return toResponse(app, "Application retrieved successfully");
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);

        List<JobApplication> apps = applicationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        for (String s : List.of("SAVED", "READY_TO_APPLY", "APPLIED", "INTERVIEW", "OFFER", "REJECTED")) {
            statusBreakdown.put(s, 0L);
        }
        for (JobApplication app : apps) {
            statusBreakdown.merge(app.getStatus(), 1L, Long::sum);
        }

        long savedJobsCount = savedJobRepository.findByUserIdOrderBySavedAtDesc(user.getId()).size();
        long uploadedResumesCount = resumeRepository.findByUserIdOrderByUploadedAtDesc(user.getId()).size();

        List<JobMatch> matches = jobMatchRepository.findByUserIdOrderByCalculatedAtDesc(user.getId());
        double avgScore = matches.isEmpty()
                ? 0.0
                : Math.round(matches.stream().mapToInt(JobMatch::getMatchScore).average().orElse(0.0) * 10.0) / 10.0;

        LocalDate today = LocalDate.now();
        List<JobApplicationResponse> upcomingFollowUps = apps.stream()
                .filter(a -> a.getFollowUpDate() != null && !a.getFollowUpDate().isBefore(today))
                .sorted(Comparator.comparing(JobApplication::getFollowUpDate))
                .limit(5)
                .map(a -> toResponse(a, "Upcoming follow-up"))
                .toList();

        List<JobApplicationResponse> recentApps = apps.stream()
                .limit(5)
                .map(a -> toResponse(a, "Recent application"))
                .toList();

        return new DashboardSummaryResponse(
                apps.size(),
                statusBreakdown,
                savedJobsCount,
                uploadedResumesCount,
                avgScore,
                upcomingFollowUps,
                recentApps
        );
    }

    private Resume resolveResume(User user, Long resumeId) {
        if (resumeId != null) {
            return resumeRepository.findByIdAndUserId(resumeId, user.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Resume not found"
                    ));
        }
        List<Resume> resumes = resumeRepository.findByUserIdOrderByUploadedAtDesc(user.getId());
        return resumes.isEmpty() ? null : resumes.get(0);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "SAVED";
        }
        String upper = status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(upper)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status. Allowed statuses: " + String.join(", ", ALLOWED_STATUSES)
            );
        }
        return upper;
    }

    private void appendHistory(JobApplication app, String transition, String detail) {
        String entry = Instant.now() + " | " + transition + " | " + detail;
        if (app.getStatusHistoryLog() == null || app.getStatusHistoryLog().isBlank()) {
            app.setStatusHistoryLog(entry);
        } else {
            app.setStatusHistoryLog(app.getStatusHistoryLog() + "\n" + entry);
        }
    }

    private String buildCoverNote(User user, Job job, String verifiedSkills) {
        String skillsText = (verifiedSkills != null && !verifiedSkills.isBlank())
                ? verifiedSkills
                : "software development and problem-solving";
        return "Dear Hiring Team at " + job.getCompany() + ",\n\n"
                + "I am writing to express my interest in the " + job.getTitle() + " position. "
                + "With verified experience in " + skillsText
                + (user.getExperienceYears() != null && user.getExperienceYears() > 0
                        ? " and " + user.getExperienceYears() + " year(s) of experience"
                        : "")
                + ", I am excited about contributing to your engineering goals.\n\n"
                + "Sincerely,\n" + user.getName() + "\n" + user.getEmail();
    }

    private String formatPrefillText(ApplicationPrefillPreviewResponse p) {
        return "Name: " + p.getCandidateName()
                + "\nEmail: " + p.getCandidateEmail()
                + "\nPhone: " + (p.getCandidatePhone() != null ? p.getCandidatePhone() : "N/A")
                + "\nLocation: " + (p.getCandidateLocation() != null ? p.getCandidateLocation() : "N/A")
                + "\nVerified Skills: " + (p.getVerifiedSkills() != null ? p.getVerifiedSkills() : "N/A")
                + "\nDirect Job URL: " + p.getDirectApplicationUrl();
    }

    private String combineCsv(String a, String b) {
        if ((a == null || a.isBlank()) && (b == null || b.isBlank())) return "";
        if (a == null || a.isBlank()) return b.trim();
        if (b == null || b.isBlank()) return a.trim();
        return a.trim() + ", " + b.trim();
    }

    private String nonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return "";
    }

    private String trimToNull(String val) {
        if (val == null || val.isBlank()) return null;
        return val.trim();
    }

    private User getAuthenticatedUser(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first"
            );
        }
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    private List<String> splitLines(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(text.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private JobApplicationResponse toResponse(JobApplication app, String message) {
        return new JobApplicationResponse(
                app.getId(),
                app.getJob().getId(),
                app.getJob().getTitle(),
                app.getJob().getCompany(),
                app.getJob().getLocation(),
                app.getResume() != null ? app.getResume().getId() : null,
                app.getStatus(),
                app.getApplicationDate(),
                app.getFollowUpDate(),
                app.getJobSource(),
                app.getNotes(),
                app.getPrefilledDataPreview(),
                app.getSubmissionConfirmation(),
                app.getSubmittedAt(),
                splitLines(app.getStatusHistoryLog()),
                app.getCreatedAt(),
                app.getUpdatedAt(),
                message
        );
    }
}
