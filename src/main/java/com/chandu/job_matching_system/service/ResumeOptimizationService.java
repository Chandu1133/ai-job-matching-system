package com.chandu.job_matching_system.service;

import com.chandu.job_matching_system.dto.ResumeOptimizationApprovalRequest;
import com.chandu.job_matching_system.dto.ResumeOptimizationResponse;
import com.chandu.job_matching_system.entity.Job;
import com.chandu.job_matching_system.entity.Resume;
import com.chandu.job_matching_system.entity.ResumeOptimization;
import com.chandu.job_matching_system.entity.User;
import com.chandu.job_matching_system.repository.JobRepository;
import com.chandu.job_matching_system.repository.ResumeOptimizationRepository;
import com.chandu.job_matching_system.repository.ResumeRepository;
import com.chandu.job_matching_system.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class ResumeOptimizationService {

    private final ResumeOptimizationRepository optimizationRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final JobMatchingService jobMatchingService;

    public ResumeOptimizationService(
            ResumeOptimizationRepository optimizationRepository,
            ResumeRepository resumeRepository,
            JobRepository jobRepository,
            UserRepository userRepository,
            JobMatchingService jobMatchingService) {
        this.optimizationRepository = optimizationRepository;
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.jobMatchingService = jobMatchingService;
    }

    @Transactional
    public ResumeOptimizationResponse generateOptimizationPreview(
            Long resumeId,
            Long jobId,
            String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job listing not found"
                ));

        String combinedSkillsCsv = combineCsv(user.getSkills(), resume.getExtractedSkills());
        String combinedResumeText = combineSections(
                resume.getExtractedText(),
                resume.getExtractedEducation(),
                resume.getExtractedExperience(),
                resume.getExtractedProjects()
        );

        JobMatchingService.MatchCalculationResult matchResult = jobMatchingService.calculateLocally(
                combinedSkillsCsv,
                user.getExperienceYears(),
                nonNull(resume.getExtractedEducation(), user.getEducation()),
                combinedResumeText,
                job
        );

        List<String> allVerifiedCandidateSkills = jobMatchingService.extractAndNormalizeSkills(
                combinedSkillsCsv,
                combinedResumeText
        );

        List<String> supportedKeywords = matchResult.matchedSkills();
        List<String> missingSkills = matchResult.missingSkills();
        List<String> suggestions = buildTruthfulSuggestions(
                supportedKeywords,
                missingSkills,
                resume,
                job
        );

        String revisedPreview = buildTruthfulRevisedPreview(
                user,
                resume,
                job,
                supportedKeywords,
                allVerifiedCandidateSkills
        );

        ResumeOptimization optimization = new ResumeOptimization();
        optimization.setUser(user);
        optimization.setResume(resume);
        optimization.setJob(job);
        optimization.setSupportedKeywords(String.join(", ", supportedKeywords));
        optimization.setMissingSkills(String.join(", ", missingSkills));
        optimization.setSuggestions(String.join("\n", suggestions));
        optimization.setRevisedResumePreview(revisedPreview);
        optimization.setStatus("PREVIEW_PENDING_APPROVAL");
        optimization.setApprovedByCandidate(false);
        optimization.setCreatedAt(Instant.now());

        ResumeOptimization saved = optimizationRepository.save(optimization);

        return toResponse(
                saved,
                "Resume optimization preview generated using only your verified qualifications. Please review and approve before saving."
        );
    }

    @Transactional
    public ResumeOptimizationResponse approveOptimization(
            Long optimizationId,
            ResumeOptimizationApprovalRequest request,
            String authenticatedEmail) {

        User user = getAuthenticatedUser(authenticatedEmail);
        ResumeOptimization optimization = optimizationRepository.findByIdAndUserId(optimizationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume optimization record not found"
                ));

        if (request != null && request.getRevisedResumePreview() != null
                && !request.getRevisedResumePreview().isBlank()) {
            optimization.setRevisedResumePreview(request.getRevisedResumePreview().trim());
        }

        optimization.setApprovedByCandidate(true);
        optimization.setStatus("APPROVED");
        optimization.setApprovedAt(Instant.now());

        if (request != null && request.isApplyToOriginalResume()) {
            Resume resume = optimization.getResume();
            resume.setExtractedText(optimization.getRevisedResumePreview());
            resume.setReviewedByCandidate(true);
            resume.setExtractionStatus("OPTIMIZED_AND_APPROVED");
            resume.setUpdatedAt(Instant.now());
            resumeRepository.save(resume);
        }

        ResumeOptimization saved = optimizationRepository.save(optimization);
        return toResponse(
                saved,
                "Resume optimization approved by candidate."
        );
    }

    @Transactional(readOnly = true)
    public List<ResumeOptimizationResponse> getMyOptimizations(String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        return optimizationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(opt -> toResponse(opt, "Optimization retrieved"))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeOptimizationResponse getOptimizationById(Long id, String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        ResumeOptimization opt = optimizationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume optimization record not found"
                ));
        return toResponse(opt, "Optimization retrieved successfully");
    }

    @Transactional
    public void deleteOptimization(Long id, String authenticatedEmail) {
        User user = getAuthenticatedUser(authenticatedEmail);
        ResumeOptimization opt = optimizationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume optimization record not found"
                ));
        optimizationRepository.delete(opt);
        optimizationRepository.flush();
    }

    private List<String> buildTruthfulSuggestions(
            List<String> supportedKeywords,
            List<String> missingSkills,
            Resume resume,
            Job job) {

        List<String> list = new ArrayList<>();

        if (!supportedKeywords.isEmpty()) {
            list.add("Highlight your verified job-relevant skills (" + String.join(", ", supportedKeywords)
                    + ") near the top of your Technical Skills and Summary sections to align with "
                    + job.getTitle() + " at " + job.getCompany() + ".");
        }

        if (!missingSkills.isEmpty()) {
            list.add("Underrepresented/Missing Skills (" + String.join(", ", missingSkills)
                    + "): Only add these if you genuinely have hands-on experience with them. Do not list unverified skills; instead, emphasize transferable foundations from your existing projects.");
        }

        if (resume.getExtractedExperience() == null || resume.getExtractedExperience().isBlank()) {
            list.add("Add a dedicated 'Work Experience' or 'Internships & Academic Projects' section using action verbs (e.g., 'Designed', 'Implemented', 'Tested') to describe your real contributions.");
        } else {
            list.add("Structure your existing experience bullets using the 'Action Verb + Technical Skill + Measurable Outcome' format while preserving factual accuracy.");
        }

        if (resume.getExtractedProjects() == null || resume.getExtractedProjects().isBlank()) {
            list.add("Include concrete technical projects that demonstrate how you applied your verified skills in practice.");
        }

        return list;
    }

    private String buildTruthfulRevisedPreview(
            User user,
            Resume resume,
            Job job,
            List<String> supportedKeywords,
            List<String> allVerifiedCandidateSkills) {

        List<String> orderedSkills = new ArrayList<>(supportedKeywords);
        for (String skill : allVerifiedCandidateSkills) {
            if (orderedSkills.stream().noneMatch(s -> s.equalsIgnoreCase(skill))) {
                orderedSkills.add(skill);
            }
        }

        String educationText = nonNull(resume.getExtractedEducation(), user.getEducation(), "Not specified — please review and add your verified education details.");
        String experienceText = nonNull(resume.getExtractedExperience(), "Not specified — please review and add your verified internships or work experience.");
        String projectsText = nonNull(resume.getExtractedProjects(), "Not specified — please review and add your verified technical projects.");

        StringBuilder sb = new StringBuilder();
        sb.append("=== TAILORED RESUME PREVIEW (TRUTHFUL & VERIFIED ONLY) ===\n");
        sb.append("Candidate: ").append(user.getName()).append("\n");
        sb.append("Email: ").append(user.getEmail()).append("\n");
        if (user.getLocation() != null && !user.getLocation().isBlank()) {
            sb.append("Location: ").append(user.getLocation()).append("\n");
        }
        sb.append("Target Role: ").append(job.getTitle()).append(" (").append(job.getCompany()).append(")\n\n");

        sb.append("PROFESSIONAL SUMMARY\n");
        if (!supportedKeywords.isEmpty()) {
            sb.append("Motivated candidate with verified proficiency in ")
              .append(String.join(", ", supportedKeywords));
            if (user.getExperienceYears() != null && user.getExperienceYears() > 0) {
                sb.append(" and ").append(user.getExperienceYears()).append(" year(s) of experience");
            }
            sb.append(", seeking to contribute to the ").append(job.getTitle())
              .append(" role at ").append(job.getCompany()).append(".\n\n");
        } else {
            sb.append("Motivated candidate seeking to apply verified software engineering and problem-solving skills to the ")
              .append(job.getTitle()).append(" role at ").append(job.getCompany()).append(".\n\n");
        }

        sb.append("VERIFIED TECHNICAL SKILLS (Prioritized for Target Role)\n");
        sb.append(orderedSkills.isEmpty() ? "None listed yet" : String.join(", ", orderedSkills)).append("\n\n");

        sb.append("WORK EXPERIENCE\n");
        sb.append(experienceText).append("\n\n");

        sb.append("KEY PROJECTS\n");
        sb.append(projectsText).append("\n\n");

        sb.append("EDUCATION\n");
        sb.append(educationText).append("\n");

        return sb.toString();
    }

    private String combineCsv(String a, String b) {
        if ((a == null || a.isBlank()) && (b == null || b.isBlank())) {
            return "";
        }
        if (a == null || a.isBlank()) return b.trim();
        if (b == null || b.isBlank()) return a.trim();
        return a.trim() + ", " + b.trim();
    }

    private String combineSections(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (!sb.isEmpty()) sb.append("\n");
                sb.append(p.trim());
            }
        }
        return sb.toString();
    }

    private String nonNull(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
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

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private List<String> splitLines(String lines) {
        if (lines == null || lines.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(lines.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private ResumeOptimizationResponse toResponse(ResumeOptimization opt, String message) {
        return new ResumeOptimizationResponse(
                opt.getId(),
                opt.getResume().getId(),
                opt.getResume().getOriginalFileName(),
                opt.getJob().getId(),
                opt.getJob().getTitle(),
                opt.getJob().getCompany(),
                splitCsv(opt.getSupportedKeywords()),
                splitCsv(opt.getMissingSkills()),
                splitLines(opt.getSuggestions()),
                opt.getRevisedResumePreview(),
                opt.getStatus(),
                opt.isApprovedByCandidate(),
                opt.getCreatedAt(),
                opt.getApprovedAt(),
                message
        );
    }
}
